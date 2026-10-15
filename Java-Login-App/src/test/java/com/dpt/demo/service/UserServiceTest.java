package com.dpt.demo.service;

import com.dpt.demo.dto.UserLoginRequest;
import com.dpt.demo.dto.UserRegistrationRequest;
import com.dpt.demo.model.User;
import com.dpt.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService} with a mocked {@link UserRepository}
 * and a real {@link BCryptPasswordEncoder} - no database, fully deterministic.
 */
class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    private UserRegistrationRequest validRequest() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setFirstName("Aarav");
        request.setLastName("Sharma");
        request.setEmail("aarav@example.com");
        request.setUserName("aarav");
        request.setPassword("Password1");
        request.setConfirmPassword("Password1");
        return request;
    }

    @Test
    void registerStoresBcryptHashNeverPlaintext() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(), any(), any(), any(), any())).thenReturn(1L);

        UserService.Result result = userService.register(validRequest());

        assertFalse(result.hasErrors());
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(userRepository).save(eq("Aarav"), eq("Sharma"), eq("aarav@example.com"),
                eq("aarav"), hashCaptor.capture());
        String stored = hashCaptor.getValue();
        assertTrue(stored.startsWith("$2"), "stored value must be a BCrypt hash");
        assertNotEquals("Password1", stored);
        assertTrue(passwordEncoder.matches("Password1", stored));
    }

    @Test
    void registerTrimsTextInputsBeforeValidationAndStorage() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(), any(), any(), any(), any())).thenReturn(1L);

        UserRegistrationRequest request = validRequest();
        request.setUserName("  aarav  ");
        request.setFirstName("  Aarav ");
        request.setEmail("  aarav@example.com  ");
        assertFalse(userService.register(request).hasErrors());

        verify(userRepository).save(eq("Aarav"), eq("Sharma"), eq("aarav@example.com"),
                eq("aarav"), anyString());
    }

    @Test
    void registerRejectsMissingRequiredFields() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setPassword("");

        UserService.Result result = userService.register(request);

        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().containsKey("firstName"));
        assertTrue(result.getErrors().containsKey("userName"));
        assertTrue(result.getErrors().containsKey("password"));
        assertTrue(result.getErrors().containsKey("confirmPassword"));
        verify(userRepository, never()).save(any(), any(), any(), any(), any());
    }

    @Test
    void registerRejectsInvalidEmail() {
        UserRegistrationRequest request = validRequest();
        request.setEmail("not-an-email-at-all");

        UserService.Result result = userService.register(request);

        assertTrue(result.getErrors().containsKey("email"));
        verify(userRepository, never()).save(any(), any(), any(), any(), any());
    }

    @Test
    void registerAcceptsBlankEmailButRejectsWeakPasswords() {
        UserRegistrationRequest blankEmail = validRequest();
        blankEmail.setEmail("");
        assertFalse(userService.register(blankEmail).hasErrors());

        UserRegistrationRequest shortPassword = validRequest();
        shortPassword.setPassword("Ab1");
        shortPassword.setConfirmPassword("Ab1");
        assertTrue(userService.register(shortPassword).getErrors().containsKey("password"));

        UserRegistrationRequest noDigit = validRequest();
        noDigit.setPassword("OnlyLetters");
        noDigit.setConfirmPassword("OnlyLetters");
        assertTrue(userService.register(noDigit).getErrors().containsKey("password"));
    }

    @Test
    void registerRejectsPasswordConfirmationMismatch() {
        UserRegistrationRequest request = validRequest();
        request.setConfirmPassword("Different1");

        UserService.Result result = userService.register(request);

        assertTrue(result.getErrors().containsKey("confirmPassword"));
        verify(userRepository, never()).save(any(), any(), any(), any(), any());
    }

    @Test
    void registerRejectsDuplicateUsernameWithFieldLevelError() {
        when(userRepository.existsByUsername("aarav")).thenReturn(true);

        UserService.Result result = userService.register(validRequest());

        assertTrue(result.getErrors().containsKey("userName"));
        verify(userRepository, never()).save(any(), any(), any(), any(), any());
    }

    @Test
    void registerRejectsDuplicateEmailWithFieldLevelError() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("aarav@example.com")).thenReturn(true);

        UserService.Result result = userService.register(validRequest());

        assertTrue(result.getErrors().containsKey("email"));
        verify(userRepository, never()).save(any(), any(), any(), any(), any());
    }

    @Test
    void registerTurnsDatabaseFailureIntoFriendlyErrorWithoutLeakingDetails() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(), any(), any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("connection refused to mysql-prod"));

        UserService.Result result = userService.register(validRequest());

        assertTrue(result.hasErrors());
        String message = result.getErrors().get("form");
        assertNotNull(message);
        assertFalse(message.contains("mysql-prod"), "internal details must not leak to users");
    }

    @Test
    void registerTranslatesUniqueConstraintViolationRace() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(), any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key username"));

        UserService.Result result = userService.register(validRequest());

        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().get("form").toLowerCase().contains("already registered"));
    }

    @Test
    void loginSucceedsWhenPasswordMatchesStoredHash() {
        String hash = passwordEncoder.encode("Password1");
        User user = new User(1L, "Aarav", "Sharma", "aarav@example.com", "aarav", hash);
        when(userRepository.findByUsernameOrEmail("aarav")).thenReturn(user);

        UserLoginRequest login = new UserLoginRequest();
        login.setUserName("aarav");
        login.setPassword("Password1");

        assertFalse(userService.login(login).hasError());
    }

    @Test
    void loginFailsWithSameGenericErrorForUnknownUserAndWrongPassword() {
        UserLoginRequest unknown = new UserLoginRequest();
        unknown.setUserName("ghost");
        unknown.setPassword("Whatever1");
        String unknownError = userService.login(unknown).getError();
        assertEquals("Invalid username or password.", unknownError);

        String hash = passwordEncoder.encode("Password1");
        User user = new User(1L, "Aarav", "Sharma", "aarav@example.com", "aarav", hash);
        when(userRepository.findByUsernameOrEmail("aarav")).thenReturn(user);

        UserLoginRequest wrong = new UserLoginRequest();
        wrong.setUserName("aarav");
        wrong.setPassword("WrongPassword1");
        UserService.LoginResult wrongResult = userService.login(wrong);

        assertTrue(wrongResult.hasError());
        assertEquals(unknownError, wrongResult.getError(),
                "error must be identical to prevent username enumeration");
    }

    @Test
    void loginRejectsBlankCredentialsWithoutTouchingTheDatabase() {
        UserService.LoginResult result = userService.login(new UserLoginRequest());

        assertTrue(result.hasError());
        assertEquals("Invalid username or password.", result.getError());
        verify(userRepository, never()).findByUsernameOrEmail(any());
    }

    @Test
    void getUserReturnsSafeProjectionWithoutPasswordHash() {
        String hash = passwordEncoder.encode("Password1");
        User user = new User(1L, "Aarav", "Sharma", "aarav@example.com", "aarav", hash);
        when(userRepository.findByUsernameOrEmail("aarav")).thenReturn(user);

        com.dpt.demo.dto.User authenticated = userService.getUser("aarav");

        assertNotNull(authenticated);
        assertEquals(Long.valueOf(1L), authenticated.getId());
        assertEquals("Aarav", authenticated.getFirstName());
        assertEquals("Sharma", authenticated.getLastName());
        assertEquals("aarav@example.com", authenticated.getEmail());
        assertEquals("aarav", authenticated.getUserName());
        // dto.User has no password accessor by design - the hash cannot leak.
    }

    @Test
    void getUserReturnsNullForUnknownOrUnreachableAccounts() {
        when(userRepository.findByUsernameOrEmail("ghost")).thenReturn(null);
        assertNull(userService.getUser("ghost"));

        when(userRepository.findByUsernameOrEmail("down"))
                .thenThrow(new DataAccessResourceFailureException("db down"));
        assertNull(userService.getUser("down"));
    }
}