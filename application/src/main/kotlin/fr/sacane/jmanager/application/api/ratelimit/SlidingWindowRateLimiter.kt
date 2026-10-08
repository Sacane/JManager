package fr.sacane.jmanager.application.api.ratelimit

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.ArrayDeque

/**
 * Counts events per key over a sliding [window] and refuses a key once [limit] events fall within it.
 * In memory: the count holds for one instance, as the session store already assumes.
 */
class SlidingWindowRateLimiter(
    private val limit: Int,
    private val window: Duration,
    private val clock: Clock,
) {
    private val events = HashMap<String, ArrayDeque<Instant>>()

    /** Whether one more event for [key] would stay within the limit. Records nothing. */
    fun isAllowed(key: String): Boolean = synchronized(this) {
        recentEvents(key, clock.instant()).size < limit
    }

    /** Records an event for [key], whether or not it was allowed. */
    fun record(key: String): Unit = synchronized(this) {
        val now = clock.instant()
        recentEvents(key, now).addLast(now)
    }

    /** Records an event for [key] only if it is allowed; a refused attempt is not counted. */
    fun tryAcquire(key: String): Boolean = synchronized(this) {
        val now = clock.instant()
        val recent = recentEvents(key, now)
        if (recent.size >= limit) return false
        recent.addLast(now)
        true
    }

    fun clear(key: String): Unit = synchronized(this) {
        events.remove(key)
    }

    /** Forgets every key whose events have all left the window. */
    fun evictExpired(): Unit = synchronized(this) {
        val now = clock.instant()
        events.entries.removeIf { (key, _) -> recentEvents(key, now).isEmpty() }
    }

    internal fun trackedKeys(): Set<String> = synchronized(this) { events.keys.toSet() }

    private fun recentEvents(key: String, now: Instant): ArrayDeque<Instant> {
        val recent = events.getOrPut(key) { ArrayDeque() }
        val oldestKept = now.minus(window)
        while (recent.isNotEmpty() && !recent.first.isAfter(oldestKept)) recent.removeFirst()
        return recent
    }
}
