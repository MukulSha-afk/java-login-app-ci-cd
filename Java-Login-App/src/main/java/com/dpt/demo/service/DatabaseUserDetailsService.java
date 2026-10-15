package com.dpt.demo.service;

import com.dpt.demo.model.User;
import com.dpt.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Locale;

/**
 * Loads accounts for Spring Security form login.
 *
 * <p>Looks the account up by username <em>or</em> email (case-insensitive) through the
 * existing {@link UserRepository} and returns the stored BCrypt hash as the
 * {@link UserDetails} password so {@code BCryptPasswordEncoder} performs the only
 * accepted verification. A plaintext comparison fallback is intentionally not provided.
 *
 * <p>Accounts that exceeded the failed-attempt budget are locked for a cool-down
 * period via {@link LoginAttemptService}. The thrown {@link LockedException} surfaces
 * as the same generic login error used for invalid credentials, so no extra
 * information is disclosed.
 */
public class DatabaseUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUserDetailsService.class);

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;

    public DatabaseUserDetailsService(UserRepository userRepository,
                                      LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String identifier = username == null ? "" : username.trim();
        String key = identifier.toLowerCase(Locale.ROOT);

        if (loginAttemptService.isBlocked(key)) {
            // Generic lockout: the failure handler redirects to the same
            // "invalid_credentials" error used for any bad login.
            throw new LockedException("Too many failed login attempts.");
        }

        User user;
        try {
            user = userRepository.findByUsernameOrEmail(identifier);
        } catch (DataAccessException ex) {
            // Do not leak storage details; log server-side only (no passwords involved).
            log.error("Login lookup failed for identifier '{}': {}", identifier, ex.getMessage());
            throw new UsernameNotFoundException("Login is temporarily unavailable.", ex);
        }

        if (user == null) {
            throw new UsernameNotFoundException("User not found.");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserName())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
    }
}
