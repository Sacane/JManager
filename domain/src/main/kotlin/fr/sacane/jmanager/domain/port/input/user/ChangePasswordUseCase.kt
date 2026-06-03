package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.Hasher
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure

data class ChangePasswordCommand(
    val userId: UserId,
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String,
) : Command<Unit>

@Port(Side.APPLICATION)
interface ChangePasswordUseCase : CommandHandler<ChangePasswordCommand, Unit> {
    override val commandClass get() = ChangePasswordCommand::class
}

@DomainService
class ChangePasswordService(
    private val userRepository: UserRepository,
    private val hasher: Hasher,
) : ChangePasswordUseCase {

    override fun handle(command: ChangePasswordCommand): Result<Unit> {
        val stored = userRepository.findByIdWithEncodedPassword(command.userId)
            ?: return failure(
                ResultState.USER_NOT_FOUND,
                DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.password.user_not_found", "L'utilisateur est introuvable"),
            )

        if (!hasher.verify(command.currentPassword, stored.password)) {
            return failure(
                ResultState.USER_UNAUTHORIZED,
                DomainError(ResultState.USER_UNAUTHORIZED.code, "domain.user.password.invalid_credentials", "Le mot de passe actuel est incorrect"),
            )
        }

        if (command.newPassword != command.confirmPassword) {
            return failure(
                ResultState.PASSWORD_NOT_MATCH,
                DomainError(ResultState.PASSWORD_NOT_MATCH.code, "domain.user.password.mismatch", "Les mots de passe ne correspondent pas"),
            )
        }

        if (hasher.verify(command.newPassword, stored.password)) {
            return failure(
                ResultState.PASSWORD_UNCHANGED,
                DomainError(ResultState.PASSWORD_UNCHANGED.code, "domain.user.password.unchanged", "Le nouveau mot de passe doit être différent de l'ancien"),
            )
        }

        return userRepository.updatePassword(
            userId = command.userId,
            hashedPassword = hasher.hash(command.newPassword),
            clearMustChange = false,
        )
    }
}
