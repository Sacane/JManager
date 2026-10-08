package fr.sacane.jmanager.application.api.password

import fr.sacane.jmanager.application.api.ratelimit.SlidingWindowRateLimiter
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

/**
 * Limits the password reset endpoints. Every call counts, whatever its outcome:
 * - requests per address stop anyone flooding a victim's mailbox, and answer the same whether the
 *   address is registered or not;
 * - requests per client stop enumeration from one client;
 * - token checks per client stop brute force on the token.
 */
@Component
class PasswordResetRateLimiter(clock: Clock) {

    private val requestsPerAddress = SlidingWindowRateLimiter(limit = 3, window = Duration.ofHours(1), clock = clock)
    private val requestsPerClient = SlidingWindowRateLimiter(limit = 5, window = Duration.ofMinutes(15), clock = clock)
    private val tokenChecksPerClient = SlidingWindowRateLimiter(limit = 10, window = Duration.ofMinutes(15), clock = clock)

    /** Whether [client] may request a reset for [email]; an allowed request is counted against both. */
    fun tryRequest(client: String, email: String): Boolean = synchronized(this) {
        val address = email.trim().lowercase()
        if (!requestsPerClient.isAllowed(client) || !requestsPerAddress.isAllowed(address)) return false
        requestsPerClient.record(client)
        requestsPerAddress.record(address)
        true
    }

    /** Whether [client] may check or use a reset token; validate and confirm share one count. */
    fun tryTokenCheck(client: String): Boolean = tokenChecksPerClient.tryAcquire(client)

    @Scheduled(fixedDelay = 15 * 60 * 1000L)
    fun evictExpired() {
        requestsPerAddress.evictExpired()
        requestsPerClient.evictExpired()
        tokenChecksPerClient.evictExpired()
    }
}
