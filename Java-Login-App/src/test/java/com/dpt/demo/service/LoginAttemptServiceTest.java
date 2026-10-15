package com.dpt.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the in-memory failed-login throttle.
 *
 * <p>Uses the package-private constructor with an explicit lockout window in
 * milliseconds so expiry can be exercised with a short, deterministic sleep.
 */
class LoginAttemptServiceTest {

    @Test
    void unknownKeyIsNeverBlocked() {
        LoginAttemptService service = new LoginAttemptService(3, 60_000L);

        assertFalse(service.isBlocked("alice"));
        assertEquals(0, service.failureCount("alice"));
    }

    @Test
    void locksOnlyAfterMaxAttemptsNotSooner() {
        LoginAttemptService service = new LoginAttemptService(3, 60_000L);

        service.loginFailed("alice");
        assertFalse(service.isBlocked("alice"), "1 failure must not lock a budget of 3");
        service.loginFailed("alice");
        assertFalse(service.isBlocked("alice"), "2 failures must not lock a budget of 3");
        assertEquals(2, service.failureCount("alice"));

        service.loginFailed("alice");
        assertTrue(service.isBlocked("alice"), "the 3rd failure engages the lock");
    }

    @Test
    void successfulLoginResetsTheCounter() {
        LoginAttemptService service = new LoginAttemptService(3, 60_000L);
        service.loginFailed("alice");
        service.loginFailed("alice");

        service.loginSucceeded("alice");

        assertEquals(0, service.failureCount("alice"));
        assertFalse(service.isBlocked("alice"));
        // The next failure starts a fresh budget instead of continuing at 2.
        service.loginFailed("alice");
        assertFalse(service.isBlocked("alice"));
    }

    @Test
    void lockoutExpiresAfterTheConfiguredWindow() throws InterruptedException {
        LoginAttemptService service = new LoginAttemptService(1, 1_000L);

        service.loginFailed("alice");
        assertTrue(service.isBlocked("alice"), "budget of 1 locks immediately");

        Thread.sleep(1_100L);
        assertFalse(service.isBlocked("alice"), "lock must lapse after the window");
    }

    @Test
    void keysAreTrackedIndependently() {
        LoginAttemptService service = new LoginAttemptService(2, 60_000L);

        service.loginFailed("alice");
        service.loginFailed("alice");

        assertTrue(service.isBlocked("alice"));
        assertFalse(service.isBlocked("bob"), "bob must not inherit alice's lock");
        assertEquals(0, service.failureCount("bob"));
    }

    @Test
    void keysAreOpaqueTuplesCallersNormalizeCase() {
        // Lowercasing happens in SecurityConfig/DatabaseUserDetailsService;
        // the service itself must not guess.
        LoginAttemptService service = new LoginAttemptService(1, 60_000L);

        service.loginFailed("alice@example.com");

        assertTrue(service.isBlocked("alice@example.com"));
        assertFalse(service.isBlocked("ALICE@EXAMPLE.COM"));
    }
}