package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.PasswordResetToken
import fr.sacane.jmanager.domain.port.output.repository.PasswordResetTokenRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.time.Clock
import java.time.LocalDateTime

/** Resolves a raw reset token to the stored one, refusing it when unknown or expired. Never consumes it. */
@DomainService
class PasswordResetTokenVerifier(
    private val tokenRepository: PasswordResetTokenRepository,
    private val clock: Clock,
) {
    fun verify(rawToken: String): Result<PasswordResetToken> {
        val token = tokenRepository.findByTokenHash(PasswordResetToken.digestOf(rawToken))
            ?: return invalidToken()
        if (token.isExpired(LocalDateTime.now(clock))) {
            return failure(
                ResultState.PASSWORD_RESET_TOKEN_EXPIRED,
                DomainError(
                    ResultState.PASSWORD_RESET_TOKEN_EXPIRED.code,
                    "domain.user.password_reset.token_expired",
                    "Ce lien de réinitialisation a expiré",
                ),
            )
        }
        return success(token)
    }

    companion object {
        /** Unknown, consumed and replaced tokens are indistinguishable: all are simply invalid. */
        fun <T> invalidToken(): Result<T> = failure(
            ResultState.PASSWORD_RESET_TOKEN_INVALID,
            DomainError(
                ResultState.PASSWORD_RESET_TOKEN_INVALID.code,
                "domain.user.password_reset.token_invalid",
                "Ce lien de réinitialisation n'est pas valide",
            ),
        )
    }
}
