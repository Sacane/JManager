package fr.sacane.jmanager.application.api.session

import fr.sacane.jmanager.application.api.ratelimit.SlidingWindowRateLimiter
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.util.logging.Logger

/**
 * In-memory rate limiter for login attempts.
 * Blocks a client after [maxAttempts] failed attempts within a sliding [windowMillis] window.
 */
@Component
class LoginRateLimiter(
    maxAttempts: Int = 5,
    windowMillis: Long = 15 * 60 * 1000L, // 15 minutes
    clock: Clock = Clock.systemUTC(),
) {
    companion object {
        private val LOGGER = Logger.getLogger(LoginRateLimiter::class.java.name)
    }

    private val failures = SlidingWindowRateLimiter(maxAttempts, Duration.ofMillis(windowMillis), clock)

    /**
     * Check whether the given key (client address) is allowed to attempt login.
     * @return true if the attempt is allowed, false if rate-limited.
     */
    fun isAllowed(key: String): Boolean = failures.isAllowed(key)

    /**
     * Record a failed login attempt for the given key.
     */
    fun recordFailedAttempt(key: String) {
        failures.record(key)
        LOGGER.info("Recorded failed login attempt for $key")
    }

    /**
     * Clear recorded attempts for a key after successful login.
     */
    fun clearAttempts(key: String) = failures.clear(key)

    @Scheduled(fixedDelay = 15 * 60 * 1000L)
    fun evictExpired() = failures.evictExpired()
}
