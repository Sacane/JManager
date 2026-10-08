package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.PasswordPolicy
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserToken
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.usecase.PasswordRenewal
import fr.sacane.jmanager.domain.usecase.SessionOpener
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure

/** Replaces a temporary password; succeeds with a fresh session for the device that asked. */
data class ForceChangePasswordCommand(
    val userId: UserId,
    val newPassword: String,
    val confirmPassword: String,
) : Command<UserToken>

@Port(Side.APPLICATION)
interface ForceChangePasswordUseCase : CommandHandler<ForceChangePasswordCommand, UserToken> {
    override val commandClass get() = ForceChangePasswordCommand::class
}

@DomainService
class ForceChangePasswordService(
    private val userRepository: UserRepository,
    private val passwordRenewal: PasswordRenewal,
    private val sessionOpener: SessionOpener,
) : ForceChangePasswordUseCase {

    override fun handle(command: ForceChangePasswordCommand): Result<UserToken> {
        if (command.newPassword != command.confirmPassword) {
            return failure(
                ResultState.PASSWORD_NOT_MATCH,
                DomainError(ResultState.PASSWORD_NOT_MATCH.code, "domain.user.password.mismatch", "Les mots de passe ne correspondent pas"),
            )
        }

        val stored = userRepository.findByIdWithEncodedPassword(command.userId)
            ?: return failure(
                ResultState.USER_NOT_FOUND,
                DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.password.user_not_found", "L'utilisateur est introuvable"),
            )

        PasswordPolicy.violationOf<UserToken>(command.newPassword, stored.user.email)?.let { return it }

        return passwordRenewal.renew(command.userId, command.newPassword, clearMustChange = true)
            .map { sessionOpener.openFor(stored.user, stored.roles) }
    }
}
