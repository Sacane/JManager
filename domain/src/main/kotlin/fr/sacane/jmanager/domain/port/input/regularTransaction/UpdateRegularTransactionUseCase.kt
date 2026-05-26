package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID

data class UpdateRegularTransactionCommand(
    val userId: UserId,
    val regularTransaction: RegularTransaction,
    val bookletIds: List<UUID>
) : Command<RegularTransaction>

@Port(Side.APPLICATION)
interface UpdateRegularTransactionUseCase : CommandHandler<UpdateRegularTransactionCommand, RegularTransaction> {
    override val commandClass get() = UpdateRegularTransactionCommand::class
}

@DomainService
class UpdateRegularTransactionService(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val unitOfWork: UnitOfWorkTransactionProvider
) : UpdateRegularTransactionUseCase {

    override fun handle(
        command: UpdateRegularTransactionCommand
    ): Result<RegularTransaction> {
        val userId = command.userId
        return unitOfWork.executeInTransaction(command.regularTransaction) {
            val updated = regularTransactionRepository.updateRegularTransaction(userId, it, command.bookletIds)
                ?: return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction ${it.id} n'existe pas",
                    "domain.regular_transaction.update.not_found"
                )
            return@executeInTransaction success(updated)
        }
    }
}
