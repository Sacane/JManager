package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.models.PasswordResetToken
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.PasswordResetTokenRepository
import java.time.LocalDateTime

class InMemoryPasswordResetTokenRepository : PasswordResetTokenRepository {

    private val store: MutableMap<String, PasswordResetToken> = mutableMapOf()

    override fun save(token: PasswordResetToken): PasswordResetToken {
        store[token.tokenHash] = token
        return token
    }

    override fun findByTokenHash(tokenHash: String): PasswordResetToken? = store[tokenHash]

    override fun deleteByUserId(userId: UserId) {
        store.entries.removeIf { it.value.userId == userId }
    }

    override fun deleteExpired(now: LocalDateTime): Int {
        val before = store.size
        store.entries.removeIf { it.value.isExpired(now) }
        return before - store.size
    }

    // --- Test helpers ---

    fun all(): List<PasswordResetToken> = store.values.toList()

    fun clear() = store.clear()
}
