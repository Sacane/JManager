package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.*

data class DeleteRegularTransactionsCommand(
    val userId: UserId,
    val transactionIds: List<String>
) : Command<List<String>>

@Port(Side.APPLICATION)
interface DeleteRegularTransactionsUseCase : CommandHandler<DeleteRegularTransactionsCommand, List<String>> {
    override val commandClass get() = DeleteRegularTransactionsCommand::class
}

@DomainService
class DeleteRegularTransactionsService(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val unitOfWork: UnitOfWorkTransactionProvider
) : DeleteRegularTransactionsUseCase {

    override fun handle(
        command: DeleteRegularTransactionsCommand
    ): Result<List<String>> {
        val userId = command.userId
        if (command.transactionIds.isEmpty()) {
            return domainFailure(
                ResultState.TRANSACTION_ENTRY_ERROR,
                "Aucune transaction régulière à supprimer",
                "domain.regular_transaction.delete.bulk.empty_selection"
            )
        }

        val distinctIds = command.transactionIds.distinct()
        return unitOfWork.executeInTransaction(distinctIds) { ids ->
            val missingId = ids.firstOrNull {
                regularTransactionRepository.getRegularTransactionById(userId, RegularTransactionId(it)) == null
            }

            if (missingId != null) {
                return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction $missingId n'existe pas",
                    "domain.regular_transaction.delete.bulk.not_found"
                )
            }

            ids.forEach {
                regularTransactionRepository.deleteRegularTransaction(userId, RegularTransactionId(it))
            }

            return@executeInTransaction success(ids)
        }
    }
}
