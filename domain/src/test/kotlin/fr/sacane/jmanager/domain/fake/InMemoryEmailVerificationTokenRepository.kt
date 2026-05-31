package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.models.EmailVerificationToken
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.EmailVerificationTokenRepository
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

class InMemoryEmailVerificationTokenRepository : EmailVerificationTokenRepository {

    private val store: MutableMap<String, EmailVerificationToken> = mutableMapOf()

    override fun save(token: EmailVerificationToken): EmailVerificationToken {
        store[token.token] = token
        return token
    }

    override fun findByToken(token: String): EmailVerificationToken? = store[token]

    override fun deleteByUserId(userId: UserId) {
        store.entries.removeIf { it.value.userId == userId }
    }

    // --- Test helpers ---

    /** Saves a fresh (non-expired) token and returns the raw token string. */
    fun issueFreshToken(userId: UserId, clock: Clock, ttl: Duration = Duration.ofHours(24)): String {
        val raw = "fresh-token-${userId.value}"
        val token = EmailVerificationToken(raw, userId, LocalDateTime.now(clock).plus(ttl))
        save(token)
        return raw
    }

    /** Saves an already-expired token and returns the raw token string. */
    fun issueExpiredToken(userId: UserId): String {
        val raw = "expired-token-${userId.value}"
        val token = EmailVerificationToken(raw, userId, LocalDateTime.of(2000, 1, 1, 0, 0))
        save(token)
        return raw
    }

    fun hasTokenForUser(userId: UserId): Boolean = store.values.any { it.userId == userId }

    fun countForUser(userId: UserId): Int = store.values.count { it.userId == userId }

    fun existsByToken(token: String): Boolean = store.containsKey(token)

    fun clear() = store.clear()
}
