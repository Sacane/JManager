package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import org.slf4j.LoggerFactory

data class DeleteAccountCommand(val userId: UserId) : Command<Unit>

@Port(Side.APPLICATION)
interface DeleteAccountUseCase : CommandHandler<DeleteAccountCommand, Unit> {
    override val commandClass get() = DeleteAccountCommand::class
}

@DomainService
class DeleteAccountService(
    private val userRepository: UserRepository,
) : DeleteAccountUseCase {

    companion object {
        private val log = LoggerFactory.getLogger(DeleteAccountService::class.java)
    }

    override fun handle(command: DeleteAccountCommand): Result<Unit> {
        userRepository.findUserById(command.userId)
            ?: return failure(
                ResultState.USER_NOT_FOUND,
                DomainError(
                    ResultState.USER_NOT_FOUND.code,
                    "domain.user.delete.user_not_found",
                    "Le compte à supprimer est introuvable",
                )
            )

        return userRepository.deleteById(command.userId).also {
            if (it.isSuccess()) log.info("Account deleted successfully")
        }
    }
}
