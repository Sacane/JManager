package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID
import java.util.logging.Logger

data class DeleteTransactionsByIdsCommand(
    val userId: UserId,
    val bookletID: UUID,
    val transactionIds: List<UUID>
) : Command<TransactionDeletionResult>

@Port(Side.APPLICATION)
interface DeleteTransactionsByIdsUseCase : CommandHandler<DeleteTransactionsByIdsCommand, TransactionDeletionResult> {
    override val commandClass get() = DeleteTransactionsByIdsCommand::class
}

@DomainService
class DeleteTransactionsByIdsService(
    private val transactionRepository: TransactionRepository,
    private val bookletRepository: BookletRepository,
    private val infraTransactionManager: UnitOfWorkTransactionProvider,
    private val trackerRepository: RegularTransactionTrackerRepository
) : DeleteTransactionsByIdsUseCase {

    companion object {
        private val logger = Logger.getLogger(DeleteTransactionsByIdsService::class.java.name)
    }

    override fun handle(command: DeleteTransactionsByIdsCommand): Result<TransactionDeletionResult> {
        return infraTransactionManager.executeInTransaction(transactionRepository) {
            val booklet: Booklet = bookletRepository.findBookletByIdWithTransactions(command.bookletID)
                ?: return@executeInTransaction domainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Booklet ${command.bookletID} n'existe pas",
                    "domain.transaction.delete.booklet_not_found"
                )

                if (command.transactionIds.isEmpty()) {
                    return@executeInTransaction domainFailure(
                        ResultState.TRANSACTION_ENTRY_ERROR,
                        "Aucune transaction à supprimer",
                        "domain.transaction.delete.empty_selection"
                    )
                }

                val transactionsToDelete = booklet.transactions.filter { command.transactionIds.contains(it.id) }
                if (transactionsToDelete.size != command.transactionIds.size) {
                    return@executeInTransaction domainFailure(
                        ResultState.TRANSACTION_NOT_FOUND,
                        "Certaines transactions à supprimer sont introuvables pour le livret ${command.bookletID}",
                        "domain.transaction.delete.some_not_found"
                    )
                }

                transactionsToDelete.forEach { transaction ->
                    if (transaction.regularTransactionId != null) {
                        trackerRepository.markMonthAsExcluded(
                            regularTransactionId = transaction.regularTransactionId,
                            bookletId = command.bookletID,
                            year = transaction.date.year,
                            month = transaction.date.month
                        )
                        logger.info("Marked month ${transaction.date.month}/${transaction.date.year} as excluded for regular transaction ${transaction.regularTransactionId}")
                    }
                }

                transactionRepository.deleteAllTransactionsById(command.transactionIds)

                val isTransactionOnList: (s: Transaction) -> Boolean = { command.transactionIds.contains(it.id) }
                booklet.removeTransactionIf(isTransactionOnList)
                bookletRepository.update(booklet)

                return@executeInTransaction success(
                    TransactionDeletionResult(
                        deletedIds = command.transactionIds,
                        bookletAmount = booklet.amount,
                    )
                )
            }
    }
}
