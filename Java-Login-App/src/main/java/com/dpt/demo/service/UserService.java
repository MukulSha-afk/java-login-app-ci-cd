package com.dpt.demo.service;

import com.dpt.demo.dto.UserLoginRequest;
import com.dpt.demo.dto.UserRegistrationRequest;
import com.dpt.demo.model.User;
import com.dpt.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Registration and account-lookup service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Server-side validation (trimming, required fields, email format, password
 *       policy, confirmation match) with field-level error messages.</li>
 *   <li>Uniqueness enforcement for username and email, backed by database
 *       constraints that also cover concurrent duplicate submissions.</li>
 *   <li>BCrypt hashing before persistence - plaintext passwords never reach the
 *       database or the logs.</li>
 *   <li>Safe error handling: database failures become friendly field/global errors;
 *       raw SQL exceptions stay in the server-side log and never reach the browser.</li>
 * </ul>
 *
 * <p>{@link #login(UserLoginRequest)} verifies credentials exclusively through
 * {@link PasswordEncoder#matches(CharSequence, String)} against the stored BCrypt
 * hash. The historical fallback that accepted a submitted password equal to the
 * stored database value has been removed.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern HAS_LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Outcome of {@link #register(UserRegistrationRequest)}: either success (no
     * errors) or a map of field name to user-friendly message. The key
     * {@code form} holds errors that are not tied to a single field.
     */
    public static final class Result {
        private final Map<String, String> errors;

        private Result(Map<String, String> errors) {
            this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
        }

        /** Factory for a successful outcome (public so controller tests can build one). */
        public static Result ok() {
            return new Result(Collections.emptyMap());
        }

        /** Factory for a failed outcome keyed by field (or {@code form}). */
        public static Result of(Map<String, String> errors) {
            return new Result(errors);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public Map<String, String> getErrors() {
            return errors;
        }
    }

    /** Outcome of {@link #login(UserLoginRequest)} with a single generic error. */
    public static final class LoginResult {
        private final String error;

        private LoginResult(String error) {
            this.error = error;
        }

        static LoginResult success() {
            return new LoginResult(null);
        }

        static LoginResult failure() {
            return new LoginResult("Invalid username or password.");
        }

        public boolean hasError() {
            return error != null;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * Validate, hash and persist a new account.
     *
     * <p>The insert is transactional so a failed registration never leaves a
     * partially created account behind. Duplicate usernames/emails are rejected
     * both by pre-checks and by the database unique constraints; constraint
     * violations are translated into a friendly message without leaking SQL.
     */
    @Transactional
    public Result register(UserRegistrationRequest request) {
        trimInputs(request);

        Map<String, String> errors = new LinkedHashMap<>();
        validateFields(request, errors);
        if (!errors.isEmpty()) {
            return Result.of(errors);
        }

        // Uniqueness pre-check (friendly, field-level). The unique constraints in
        // schema.sql remain the authoritative guard against concurrent duplicates.
        if (userRepository.existsByUsername(request.getUserName())) {
            errors.put("userName", "That username is already taken.");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()
                && userRepository.existsByEmail(request.getEmail())) {
            errors.put("email", "An account with that email already exists.");
        }
        if (!errors.isEmpty()) {
            return Result.of(errors);
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        try {
            long id = userRepository.save(request.getFirstName(), request.getLastName(),
                    emptyToNull(request.getEmail()), request.getUserName(), passwordHash);
            if (id < 0) {
                errors.put("form", "We could not create your account right now. Please try again later.");
                return Result.of(errors);
            }
        } catch (DataIntegrityViolationException ex) {
            // Race between the pre-check and the insert; the constraint caught it.
            log.warn("Registration rejected by unique constraint for username '{}': {}",
                    request.getUserName(), ex.getMessage());
            errors.put("form", "That username or email is already registered.");
            return Result.of(errors);
        } catch (DataAccessException ex) {
            // Full stack trace stays server-side; no passwords or hashes are logged.
            log.error("Registration failed for username '{}': {}",
                    request.getUserName(), ex.getMessage(), ex);
            errors.put("form", "We could not create your account right now. Please try again later.");
            return Result.of(errors);
        }

        log.info("Registered new account for username '{}'.", request.getUserName());
        return Result.ok();
    }

    /**
     * Verify a submitted login against the stored BCrypt hash.
     *
     * <p>Missing accounts and bad passwords produce the same generic error so the
     * response cannot be used to discover valid usernames.
     */
    public LoginResult login(UserLoginRequest request) {
        if (request == null || request.getUserName() == null || request.getPassword() == null) {
            return LoginResult.failure();
        }
        String identifier = request.getUserName().trim();
        User user;
        try {
            user = userRepository.findByUsernameOrEmail(identifier);
        } catch (DataAccessException ex) {
            log.error("Login lookup failed for '{}': {}", identifier, ex.getMessage(), ex);
            return LoginResult.failure();
        }
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return LoginResult.failure();
        }
        return LoginResult.success();
    }

    /**
     * Load the safe dashboard projection for an authenticated principal.
     * The password hash is never included in the returned object.
     */
    public com.dpt.demo.dto.User getUser(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return null;
        }
        User user;
        try {
            user = userRepository.findByUsernameOrEmail(userName.trim());
        } catch (DataAccessException ex) {
            log.error("Dashboard lookup failed for '{}': {}", userName, ex.getMessage(), ex);
            return null;
        }
        if (user == null) {
            return null;
        }
        return new com.dpt.demo.dto.User(user.getId(), user.getFirstName(),
                user.getLastName(), user.getEmail(), user.getUserName());
    }

    /** Trim text inputs so uniqueness checks and storage see canonical values. */
    private static void trimInputs(UserRegistrationRequest request) {
        if (request == null) {
            return;
        }
        request.setFirstName(trimToNull(request.getFirstName()));
        request.setLastName(trimToNull(request.getLastName()));
        request.setEmail(trimToNull(request.getEmail()));
        request.setUserName(trimToNull(request.getUserName()));
        // Passwords are intentionally NOT trimmed: trailing spaces may be intentional.
    }

    private static void validateFields(UserRegistrationRequest request, Map<String, String> errors) {
        String firstName = request.getFirstName();
        if (isBlank(firstName)) {
            errors.put("firstName", "First name is required.");
        } else if (firstName.length() > 100) {
            errors.put("firstName", "First name must be 100 characters or fewer.");
        }

        String lastName = request.getLastName();
        if (lastName != null && lastName.length() > 100) {
            errors.put("lastName", "Last name must be 100 characters or fewer.");
        }

        String email = request.getEmail();
        if (email != null && !email.isEmpty()) {
            if (email.length() > 254) {
                errors.put("email", "Email must be 254 characters or fewer.");
            } else if (!EMAIL_PATTERN.matcher(email).matches()) {
                errors.put("email", "Email must be a valid email address.");
            }
        }

        String userName = request.getUserName();
        if (isBlank(userName)) {
            errors.put("userName", "Username is required.");
        } else if (userName.length() < 3 || userName.length() > 50) {
            errors.put("userName", "Username must be between 3 and 50 characters.");
        }

        String password = request.getPassword();
        if (isBlank(password)) {
            errors.put("password", "Password is required.");
        } else if (password.length() < 8 || password.length() > 100) {
            errors.put("password", "Password must be between 8 and 100 characters.");
        } else if (!HAS_LETTER.matcher(password).matches() || !HAS_DIGIT.matcher(password).matches()) {
            errors.put("password", "Password must contain at least one letter and one number.");
        }

        String confirm = request.getConfirmPassword();
        if (isBlank(confirm)) {
            errors.put("confirmPassword", "Please confirm your password.");
        } else if (password != null && !password.equals(confirm)) {
            errors.put("confirmPassword", "Passwords do not match.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}


