package fr.sacane.jmanager.domain.port.output.repository

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.PasswordResetToken
import fr.sacane.jmanager.domain.models.UserId
import java.time.LocalDateTime

/** Stores password reset tokens by digest, at most one per user. */
@Port(Side.INFRASTRUCTURE)
interface PasswordResetTokenRepository {
    fun save(token: PasswordResetToken): PasswordResetToken

    /** The token whose digest is [tokenHash], or null when none was issued, it was consumed or replaced. */
    fun findByTokenHash(tokenHash: String): PasswordResetToken?

    fun deleteByUserId(userId: UserId)

    /** Deletes every token expired at [now]; returns how many were deleted. */
    fun deleteExpired(now: LocalDateTime): Int
}
