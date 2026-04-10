package fr.sacane.jmanager.infrastructure.api.session

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * In-memory rate limiter for login attempts.
 * Blocks an IP address after [maxAttempts] failed attempts within a sliding [windowMillis] window.
 */
@Component
class LoginRateLimiter(
    private val maxAttempts: Int = 5,
    private val windowMillis: Long = 15 * 60 * 1000L // 15 minutes
) {
    companion object {
        private val LOGGER = Logger.getLogger(LoginRateLimiter::class.java.name)
    }

    private val attempts = ConcurrentHashMap<String, MutableList<Long>>()

    /**
     * Check whether the given key (IP address) is allowed to attempt login.
     * @return true if the attempt is allowed, false if rate-limited.
     */
    fun isAllowed(key: String): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = attempts.compute(key) { _, existing ->
            val list = existing ?: mutableListOf()
            list.removeAll { it < now - windowMillis }
            list
        }!!
        return timestamps.size < maxAttempts
    }

    /**
     * Record a failed login attempt for the given key.
     */
    fun recordFailedAttempt(key: String) {
        val now = System.currentTimeMillis()
        attempts.compute(key) { _, existing ->
            val list = existing ?: mutableListOf()
            list.removeAll { it < now - windowMillis }
            list.add(now)
            list
        }
        LOGGER.info("Recorded failed login attempt for $key")
    }

    /**
     * Clear recorded attempts for a key after successful login.
     */
    fun clearAttempts(key: String) {
        attempts.remove(key)
    }
}
