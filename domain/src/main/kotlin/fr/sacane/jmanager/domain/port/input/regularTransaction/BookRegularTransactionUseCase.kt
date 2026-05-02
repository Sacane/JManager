package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID

data class BookRegularTransactionCommand(
    val userId: UserId,
    val regularTransaction: RegularTransaction,
    val bookletIds: List<UUID>
) : Command<RegularTransaction>

@Port(Side.APPLICATION)
interface BookRegularTransactionUseCase : CommandHandler<BookRegularTransactionCommand, RegularTransaction> {
    override val commandClass get() = BookRegularTransactionCommand::class
}

@DomainService
class BookRegularTransactionService(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val unitOfWork: UnitOfWorkTransactionProvider
) : BookRegularTransactionUseCase {

    override fun handle(command: BookRegularTransactionCommand): Result<RegularTransaction> {
        val userId = command.userId
        return unitOfWork.executeInTransaction(command.regularTransaction) {
            val transactionWithId = it.copy(id = RegularTransactionId(UUID.randomUUID().toString()))
            val transaction = regularTransactionRepository.saveRegularTransaction(
                userId = userId,
                regularTransaction = transactionWithId,
                bookletIds = command.bookletIds
            )
            return@executeInTransaction success(transaction)
        }
    }
}
