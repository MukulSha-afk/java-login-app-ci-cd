package com.dpt.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory failed-login throttling.
 *
 * <p>Tracks consecutive failed attempts per normalized login identifier (lowercased
 * username or email). After {@code app.security.login.max-attempts} consecutive
 * failures the identifier is locked for {@code app.security.login.lockout-minutes}
 * minutes; every attempt during the lockout keeps failing with the same generic
 * login error. A successful login resets the counter.
 *
 * <p>Limitations (documented on purpose): the store is per-instance and not shared
 * across horizontally scaled nodes, and it resets on restart. For a multi-node
 * deployment the same interface should be backed by Redis or the database. This is
 * intentionally simple for the single-instance Jenkins/AWS deployment target.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final int maxAttempts;
    private final long lockoutMillis;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /**
     * Spring constructor. Boxed parameter types keep this signature distinct from
     * the package-private {@code (int, long)} test constructor.
     */
    @Autowired
    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}") Integer maxAttempts,
            @Value("${app.security.login.lockout-minutes:15}") Long lockoutMinutes) {
        this(maxAttempts == null ? 5 : maxAttempts,
                (lockoutMinutes == null ? 15L : lockoutMinutes) * 60_000L);
    }

    /** Package-private constructor with an explicit lockout window in millis. */
    LoginAttemptService(int maxAttempts, long lockoutMillis) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.lockoutMillis = Math.max(1_000L, lockoutMillis);
    }

    /** Whether the identifier is currently locked out. */
    public boolean isBlocked(String key) {
        purgeExpired();
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return false;
        }
        if (attempt.lockedUntil > System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    /** Record a failed login; locks the identifier once the budget is exhausted. */
    public void loginFailed(String key) {
        purgeExpired();
        attempts.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            // Fresh window: first failure, or the previous window/lock fully elapsed.
            boolean freshWindow = existing == null
                    || (existing.lockedUntil <= now && now - existing.windowStart >= lockoutMillis);
            int count = freshWindow ? 1 : existing.count + 1;
            long windowStart = freshWindow ? now : existing.windowStart;
            if (count >= maxAttempts) {
                log.warn("Login lockout engaged for key '{}' after {} failed attempts.", k, count);
                return new Attempt(count, now + lockoutMillis, windowStart);
            }
            // Below the budget: count the failure but do not lock yet.
            return new Attempt(count, 0L, windowStart);
        });
    }

    /** Reset the counter after a successful login. */
    public void loginSucceeded(String key) {
        attempts.remove(key);
    }

    /** Current consecutive-failure count for the key (0 when unknown). Visible for tests. */
    int failureCount(String key) {
        Attempt attempt = attempts.get(key);
        return attempt == null ? 0 : attempt.count;
    }

    /** Drop entries whose lock window has already expired to bound memory growth. */
    private void purgeExpired() {
        if (attempts.size() < 512) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Attempt>> it = attempts.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().lockedUntil <= now) {
                it.remove();
            }
        }
    }

    /** Immutable holder of one identifier's failure state. */
    private static final class Attempt {
        /** Consecutive failures inside the current tracking window. */
        private final int count;
        /** Epoch millis until which the key stays locked; 0 when not locked. */
        private final long lockedUntil;
        /** When the current failure-counting window started. */
        private final long windowStart;

        private Attempt(int count, long lockedUntil, long windowStart) {
            this.count = count;
            this.lockedUntil = lockedUntil;
            this.windowStart = windowStart;
        }
    }
}
