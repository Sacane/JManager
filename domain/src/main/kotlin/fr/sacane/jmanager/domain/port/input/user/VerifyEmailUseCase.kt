package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.port.output.repository.EmailVerificationTokenRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.time.Clock
import java.time.LocalDateTime

data class VerifyEmailCommand(val token: String) : Command<Unit>

@Port(Side.APPLICATION)
interface VerifyEmailUseCase : CommandHandler<VerifyEmailCommand, Unit> {
    override val commandClass get() = VerifyEmailCommand::class
}

@DomainService
class VerifyEmailService(
    private val tokenRepository: EmailVerificationTokenRepository,
    private val userRepository: UserRepository,
    private val clock: Clock,
) : VerifyEmailUseCase {

    override fun handle(command: VerifyEmailCommand): Result<Unit> {
        val token = tokenRepository.findByToken(command.token)
            ?: return failure(
                ResultState.NOT_FOUND,
                DomainError(ResultState.NOT_FOUND.code, "domain.user.email_verification.token_not_found", "Token de vérification inconnu")
            )
        if (token.isExpired(LocalDateTime.now(clock))) {
            return failure(
                ResultState.EMAIL_VERIFICATION_TOKEN_EXPIRED,
                DomainError(
                    ResultState.EMAIL_VERIFICATION_TOKEN_EXPIRED.code,
                    "domain.user.email_verification.token_expired",
                    "Le lien de vérification a expiré"
                )
            )
        }
        userRepository.markEmailVerified(token.userId)
        tokenRepository.deleteByUserId(token.userId)
        return success(Unit)
    }
}
