package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.models.roleUser
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.domain.utils.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.UUID
import java.util.logging.Logger

@Port(Side.APPLICATION)
/**
 * Application port: TransactionFeature
 *
 * High-level API to create, retrieve, update, and delete transactions for user booklets.
 * All operations require an authentication token and return a domain Result<T> describing
 * success or domain-specific failure states.
 */
sealed interface TransactionFeature {
    /**
     * Book (create) a transaction for the booklet identified by its label.
     *
     * @param token Authentication token identifying the requester.
     * @param bookletLabel Label of the booklet where the transaction will be added.
     * @param transaction The Transaction to persist.
     * @return Result containing a TransactionResumeResult on success, or an error state on failure.
     */
    fun bookTransaction(token: String, bookletLabel: String, transaction: Transaction): Result<TransactionResumeResult>

    /**
     * Retrieve transactions for a specific month and year for the given booklet label.
     *
     * @param token Authentication token identifying the requester.
     * @param month The month to retrieve transactions for.
     * @param year The year to retrieve transactions for.
     * @param bookletLabel The label of the booklet to fetch transactions from.
     * @return Result containing the list of Transaction objects on success, or a not found error.
     */
    fun retrieveTransactionsByMonthAndYear(token: String, month: Month, year: Int, bookletLabel: String): Result<List<Transaction>>

    /**
     * Edit an existing transaction belonging to a specific booklet.
     *
     * @param bookletID The UUID of the booklet containing the transaction.
     * @param transaction The Transaction object with updated values (must include id).
     * @param token Authentication token identifying the requester.
     * @return Result containing a TransactionResumeResult on success, or an error state on failure.
     */
    fun editTransaction(bookletID: UUID, transaction: Transaction, token: String): Result<TransactionResumeResult>

    /**
     * Find a transaction by its unique identifier.
     *
     * @param id UUID of the transaction to find.
     * @param token Authentication token identifying the requester.
     * @return Result containing the Transaction on success, or TRANSACTION_NOT_FOUND on failure.
     */
    fun findById(id: UUID, token: String): Result<Transaction>

    /**
     * Delete multiple transactions by their identifiers for a given booklet.
     *
     * @param bookletID The UUID of the booklet which owns the transactions.
     * @param transactionIds List of UUIDs corresponding to the transactions to delete.
     * @param token Authentication token identifying the requester.
     * @return Result with no value on success, or an error state if the booklet or transactions are not found.
     */
    fun deleteTransactionsByIds(bookletID: UUID, transactionIds: List<UUID>, token: String): Result<TransactionDeletionResult>

    /**
     * Confirm a provisional (preview) transaction, converting it into a real transaction.
     *
     * @param token Authentication token identifying the requester.
     * @param bookletID UUID of the booklet containing the preview transaction.
     * @param transactionId UUID of the preview transaction to confirm.
     * @return Result containing a TransactionResumeResult on success, or an appropriate failure state.
     */
    fun confirmPreviewTransaction(
        token: String,
        bookletID: UUID,
        transactionId: UUID,
        newAmount: Amount?,
        newDate: LocalDate?
    ): Result<TransactionResumeResult>
}

@DomainService
class TransactionFeatureImpl(
    private val transactionRepository: TransactionRepository,
    private val session: SessionManager,
    private val bookletRepository: BookletRepository,
    private val infraTransactionManager: UnitOfWorkTransactionProvider,
    private val tagRepository: TagRepository,
    private val trackerRepository: RegularTransactionTrackerRepository
): TransactionFeature{
    companion object {
        private val logger = Logger.getLogger(TransactionFeatureImpl::class.java.name)
    }

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    private fun <S> domainNotFound(detail: String, key: String): Result<S> {
        return domainFailure(ResultState.NOT_FOUND, detail, key)
    }

    private fun <S> domainInvalid(detail: String, key: String): Result<S> {
        return domainFailure(ResultState.INVALID, detail, key)
    }

    override fun editTransaction(
        bookletID: UUID,
        transaction: Transaction,
        token: String
    ): Result<TransactionResumeResult> = session.authenticate(token, roleUser){
        return@authenticate infraTransactionManager.executeInTransaction(transaction) {
            if (transaction.id == null) {
                return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_ENTRY_ERROR,
                    "L'ID de la transaction est null",
                    "domain.transaction.edit.id_missing"
                )
            }
            val registeredBooklet = bookletRepository.findBookletByIdWithTransactions(bookletID)
                ?: return@executeInTransaction domainNotFound(
                    "Le livret $bookletID n'existe pas",
                    "domain.transaction.edit.booklet_not_found"
                )
            val transactionFromDatabase = registeredBooklet.findTransactionById(transaction.id)?.copy()
                ?: return@executeInTransaction domainNotFound(
                    "Aucune transaction n'existe avec l'ID suivant : ${transaction.id}",
                    "domain.transaction.edit.transaction_not_found"
                )
            transactionFromDatabase.updateFromOther(transaction)
            transactionFromDatabase.lastModified = LocalDateTime.now()
            transactionRepository.save(registeredBooklet.id!!, transactionFromDatabase)
                ?: return@executeInTransaction domainInvalid(
                    "Une erreur est survenue lors de la mise à jour de la transaction ${transactionFromDatabase.id}",
                    "domain.transaction.edit.save_failed"
                )
            registeredBooklet.removeTransactionById(transaction.id)
            registeredBooklet.addTransaction(transactionFromDatabase)
            bookletRepository.update(registeredBooklet)
            success(TransactionResumeResult(transactionFromDatabase, registeredBooklet.amount))
        }
    }

    override fun bookTransaction(
        token: String,
        bookletLabel: String,
        transaction: Transaction
    ): Result<TransactionResumeResult> = session.authenticate(token) { id ->
        return@authenticate infraTransactionManager.executeInTransaction(transaction) {
            logger.info("Request for a transaction with id $id")
            val booklet = bookletRepository.findBookletByLabelWithTransactions(id, bookletLabel)
                ?: return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "Le livret $bookletLabel n'existe pas",
                    "domain.transaction.book.booklet_not_found"
                )
            val newTr =  transactionRepository.save(booklet.id!!, transaction)
                ?: return@executeInTransaction domainFailure(
                    ResultState.INFRASTRUCTURE_ERROR,
                    "Erreur est survenu lors de la transaction",
                    "domain.transaction.book.infrastructure_error"
                )
            if (transaction.amount.isNegative()) {
                return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_ENTRY_ERROR,
                    "Le montant de la transaction ne peut pas être négatif",
                    "domain.transaction.book.negative_amount"
                )
            }
            val toSaveTransaction = if (newTr.tag == null) {
                newTr.copy(
                    tag = tagRepository.defaultTag()
                )
            } else newTr
            booklet.addTransaction(toSaveTransaction)
            bookletRepository.update(booklet)
            logger.info("Transaction $newTr has been created, the booklet sold has been updated : $booklet")
            success(TransactionResumeResult(newTr, booklet.amount))
        }
    }

    override fun retrieveTransactionsByMonthAndYear(
        token: String,
        month: Month,
        year: Int,
        bookletLabel: String
    ): Result<List<Transaction>> = session.authenticate(token) {
        success(transactionRepository.findBookletByLabelWithTransactions(bookletLabel, it)?.retrieveTransactionsSortedByDate(month, year)
            ?: return@authenticate domainNotFound(
                "Aucun compte ne correspond au label indiqué",
                "domain.transaction.retrieve.booklet_not_found"
            )
        )
    }

    override fun findById(
        id: UUID,
        token: String
    ): Result<Transaction> = session.authenticate(token, roleUser) {
        logger.info("Request for a transaction with id $id")
        val transaction = transactionRepository.findTransactionById(id)
            ?: return@authenticate domainFailure(
                ResultState.TRANSACTION_NOT_FOUND,
                "La transaction $id n'existe pas",
                "domain.transaction.find.not_found"
            )
        success(transaction)
    }

    override fun deleteTransactionsByIds(bookletID: UUID, transactionIds: List<UUID>, token: String): Result<TransactionDeletionResult> {
        return session.authenticate(token) {
            infraTransactionManager.executeInTransaction(transactionRepository) {
                val booklet: Booklet = bookletRepository.findBookletByIdWithTransactions(bookletID)
                    ?: return@executeInTransaction domainFailure(
                        ResultState.BOOKLET_NOT_FOUND,
                        "Booklet $bookletID n'existe pas",
                        "domain.transaction.delete.booklet_not_found"
                    )

                if (transactionIds.isEmpty()) {
                    return@executeInTransaction domainFailure(
                        ResultState.TRANSACTION_ENTRY_ERROR,
                        "Aucune transaction à supprimer",
                        "domain.transaction.delete.empty_selection"
                    )
                }

                // Ensure all requested ids belong to this booklet before mutating balances.
                val transactionsToDelete = booklet.transactions.filter { transactionIds.contains(it.id) }
                if (transactionsToDelete.size != transactionIds.size) {
                    return@executeInTransaction domainFailure(
                        ResultState.TRANSACTION_NOT_FOUND,
                        "Certaines transactions à supprimer sont introuvables pour le livret $bookletID",
                        "domain.transaction.delete.some_not_found"
                    )
                }

                transactionsToDelete.forEach { transaction ->
                    if (transaction.regularTransactionId != null) {
                        trackerRepository.markMonthAsExcluded(
                            regularTransactionId = transaction.regularTransactionId,
                            bookletId = bookletID,
                            year = transaction.date.year,
                            month = transaction.date.month
                        )
                        logger.info("Marked month ${transaction.date.month}/${transaction.date.year} as excluded for regular transaction ${transaction.regularTransactionId}")
                    }
                }

                transactionRepository.deleteAllTransactionsById(transactionIds)

                val isTransactionOnList: (s: Transaction) -> Boolean = { transactionIds.contains(it.id) }
                booklet.removeTransactionIf(isTransactionOnList)
                bookletRepository.update(booklet)

                return@executeInTransaction success(
                    TransactionDeletionResult(
                        deletedIds = transactionIds,
                        bookletAmount = booklet.amount,
                    )
                )
            }
        }
    }

    override fun confirmPreviewTransaction(
        token: String,
        bookletID: UUID,
        transactionId: UUID,
        newAmount: Amount?,
        newDate: LocalDate?
    ): Result<TransactionResumeResult> = session.authenticate(token) {
        return@authenticate infraTransactionManager.executeInTransaction(Any()) {
            val booklet = bookletRepository.findBookletByIdWithTransactions(bookletID)
                ?: return@executeInTransaction domainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Booklet $bookletID not found",
                    "domain.transaction.confirm.booklet_not_found"
                )
            val transaction = booklet.findTransactionById(transactionId)
                ?: return@executeInTransaction domainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Transaction not found",
                    "domain.transaction.confirm.transaction_not_found"
                )

            if (transaction.isNotPreview) {
                return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_ENTRY_ERROR,
                    "Transaction $transactionId is not preview",
                    "domain.transaction.confirm.not_preview"
                )
            }

            booklet.removeTransactionById(transactionId)
            if (newAmount != null) {
                transaction.amount = newAmount
            }
            if (newDate != null) {
                transaction.date = newDate
            }
            transaction.isPreview = false

            transactionRepository.save(bookletID, transaction)
                ?: return@executeInTransaction domainFailure(
                    ResultState.INFRASTRUCTURE_ERROR,
                    "Could not confirm transaction $transactionId",
                    "domain.transaction.confirm.save_failed"
                )

            booklet.addTransaction(transaction)
            // Update only booklet balances/label to avoid JPA collection merge side-effects.
            bookletRepository.update(booklet)
            return@executeInTransaction success(TransactionResumeResult(transaction, booklet.amount))
        }
    }

}

data class TransactionDeletionResult(
    val deletedIds: List<UUID>,
    val bookletAmount: Amount,
)
