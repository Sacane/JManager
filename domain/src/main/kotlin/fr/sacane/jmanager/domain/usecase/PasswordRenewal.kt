package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.Hasher
import fr.sacane.jmanager.domain.port.output.SessionManager
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.utils.Result
import java.time.Clock
import java.time.LocalDateTime

/**
 * Sets a user's new password and revokes every session opened with the previous one.
 *
 * Every way of setting a password goes through here, so none can forget the revocation. The rules
 * that decide whether the password may be set (current password, policy, token) stay with the caller.
 */
@DomainService
class PasswordRenewal(
    private val userRepository: UserRepository,
    private val hasher: Hasher,
    private val sessionManager: SessionManager,
    private val clock: Clock,
) {
    /** @return when the password changed, in UTC, once stored and every session revoked. */
    fun renew(userId: UserId, newPassword: String, clearMustChange: Boolean): Result<LocalDateTime> {
        val changedAt = LocalDateTime.now(clock)
        return userRepository.updatePassword(
            userId = userId,
            hashedPassword = hasher.hash(newPassword),
            clearMustChange = clearMustChange,
            changedAt = changedAt,
        ).map {
            sessionManager.revokeAll(userId)
            changedAt
        }
    }
}
