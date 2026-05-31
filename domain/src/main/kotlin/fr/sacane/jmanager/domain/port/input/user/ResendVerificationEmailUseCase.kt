package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.NotificationPort
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.usecase.EmailVerificationIssuer
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success

data class ResendVerificationEmailCommand(val userId: UserId) : Command<Unit>

@Port(Side.APPLICATION)
interface ResendVerificationEmailUseCase : CommandHandler<ResendVerificationEmailCommand, Unit> {
    override val commandClass get() = ResendVerificationEmailCommand::class
}

@DomainService
class ResendVerificationEmailService(
    private val userRepository: UserRepository,
    private val notificationPort: NotificationPort,
    private val emailVerificationIssuer: EmailVerificationIssuer,
) : ResendVerificationEmailUseCase {

    override fun handle(command: ResendVerificationEmailCommand): Result<Unit> {
        val user = userRepository.findUserById(command.userId)
            ?: return failure(
                ResultState.USER_NOT_FOUND,
                DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.email_verification.user_not_found", "Utilisateur introuvable")
            )
        if (user.emailVerified) {
            return failure(
                ResultState.EMAIL_ALREADY_VERIFIED,
                DomainError(
                    ResultState.EMAIL_ALREADY_VERIFIED.code,
                    "domain.user.email_verification.already_verified",
                    "L'adresse e-mail est déjà vérifiée"
                )
            )
        }
        val email = user.email
            ?: return failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.email_verification.no_email", "Aucune adresse e-mail associée à ce compte")
            )
        val token = emailVerificationIssuer.issue(user.id)
        notificationPort.sendVerificationEmail(email, token.token)
        return success(Unit)
    }
}
