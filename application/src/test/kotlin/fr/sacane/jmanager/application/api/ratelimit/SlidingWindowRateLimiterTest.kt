package fr.sacane.jmanager.application.api.ratelimit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class SlidingWindowRateLimiterTest {

    private val clock = MovableClock(Instant.parse("2026-10-08T09:00:00Z"))
    private val limiter = SlidingWindowRateLimiter(limit = 3, window = Duration.ofMinutes(15), clock = clock)

    @Test
    fun `shouldRefuse_whenTheLimitIsReachedWithinTheWindow`() {
        repeat(3) { assertTrue(limiter.tryAcquire("key")) }

        assertFalse(limiter.tryAcquire("key"))
    }

    // A refused attempt is not recorded: hammering a closed door does not keep it closed longer.
    @Test
    fun `shouldReopenOnceTheOldestRecordedEventLeavesTheWindow`() {
        repeat(3) { limiter.tryAcquire("key") }
        clock.advance(Duration.ofMinutes(14))
        assertFalse(limiter.tryAcquire("key"))

        clock.advance(Duration.ofMinutes(1))

        assertTrue(limiter.tryAcquire("key"))
    }

    @Test
    fun `shouldCountEachKeyApart`() {
        repeat(3) { limiter.tryAcquire("a") }

        assertFalse(limiter.tryAcquire("a"))
        assertTrue(limiter.tryAcquire("b"))
    }

    @Test
    fun `shouldTellWithoutRecording_whenOnlyAsked`() {
        repeat(5) { assertTrue(limiter.isAllowed("key")) }

        repeat(3) { limiter.record("key") }

        assertFalse(limiter.isAllowed("key"))
    }

    @Test
    fun `shouldForgetAKey_whenCleared`() {
        repeat(3) { limiter.record("key") }

        limiter.clear("key")

        assertTrue(limiter.isAllowed("key"))
    }

    // Keys an attacker sprays once would otherwise stay in memory for good.
    @Test
    fun `shouldDropKeysWhoseEventsAllLeftTheWindow_whenEvicting`() {
        limiter.record("old")
        clock.advance(Duration.ofMinutes(10))
        limiter.record("recent")
        clock.advance(Duration.ofMinutes(6))

        limiter.evictExpired()

        assertEquals(setOf("recent"), limiter.trackedKeys())
    }

    private class MovableClock(private var now: Instant) : Clock() {
        fun advance(duration: Duration) {
            now = now.plus(duration)
        }

        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
    }
}
