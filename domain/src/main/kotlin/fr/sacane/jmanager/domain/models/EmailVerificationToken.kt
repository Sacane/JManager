package fr.sacane.jmanager.domain.models

import java.time.LocalDateTime

data class EmailVerificationToken(
    val token: String,
    val userId: UserId,
    val expiresAt: LocalDateTime,
) {
    fun isExpired(now: LocalDateTime): Boolean = expiresAt.isBefore(now)
}
