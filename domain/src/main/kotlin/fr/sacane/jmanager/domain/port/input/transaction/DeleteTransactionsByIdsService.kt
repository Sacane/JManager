package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID
import java.util.logging.Logger

@DomainService
class DeleteTransactionsByIdsService(
    private val transactionRepository: TransactionRepository,
    private val session: SessionManager,
    private val bookletRepository: BookletRepository,
    private val infraTransactionManager: UnitOfWorkTransactionProvider,
    private val trackerRepository: RegularTransactionTrackerRepository
) : DeleteTransactionsByIdsUseCase {

    companion object {
        private val logger = Logger.getLogger(DeleteTransactionsByIdsService::class.java.name)
    }

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(command: DeleteTransactionsByIdsCommand): Result<TransactionDeletionResult> {
        return session.authenticate(command.token) {
            infraTransactionManager.executeInTransaction(transactionRepository) {
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
}
