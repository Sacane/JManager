package fr.sacane.jmanager.application.api.session

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginRateLimiterTest {

    @Test
    fun `allows attempts until the maximum number of failures is recorded`() {
        val limiter = LoginRateLimiter(maxAttempts = 3, windowMillis = 60_000)

        repeat(2) { limiter.recordFailedAttempt("client") }
        assertTrue(limiter.isAllowed("client"))

        limiter.recordFailedAttempt("client")
        assertFalse(limiter.isAllowed("client"))
    }

    @Test
    fun `counts failures per key`() {
        val limiter = LoginRateLimiter(maxAttempts = 1, windowMillis = 60_000)

        limiter.recordFailedAttempt("client-a")

        assertFalse(limiter.isAllowed("client-a"))
        assertTrue(limiter.isAllowed("client-b"))
    }

    @Test
    fun `forgets the failures of a key once it is cleared`() {
        val limiter = LoginRateLimiter(maxAttempts = 1, windowMillis = 60_000)
        limiter.recordFailedAttempt("client")

        limiter.clearAttempts("client")

        assertTrue(limiter.isAllowed("client"))
    }

    @Test
    fun `forgets failures older than the window`() {
        val limiter = LoginRateLimiter(maxAttempts = 1, windowMillis = 50)
        limiter.recordFailedAttempt("client")
        assertFalse(limiter.isAllowed("client"))

        Thread.sleep(150)

        assertTrue(limiter.isAllowed("client"))
    }
}
