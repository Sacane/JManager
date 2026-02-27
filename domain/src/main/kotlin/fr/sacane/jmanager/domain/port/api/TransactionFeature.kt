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
     * Book (create) a transaction for the account identified by its label.
     *
     * @param token Authentication token identifying the requester.
     * @param accountLabel Label of the account (booklet) where the transaction will be added.
     * @param transaction The Transaction to persist.
     * @return Result containing a TransactionResumeResult on success, or an error state on failure.
     */
    fun bookTransaction(token: String, accountLabel: String, transaction: Transaction): Result<TransactionResumeResult>

    /**
     * Retrieve transactions for a specific month and year for the given account label.
     *
     * @param token Authentication token identifying the requester.
     * @param month The month to retrieve transactions for.
     * @param year The year to retrieve transactions for.
     * @param account The label of the account to fetch transactions from.
     * @return Result containing the list of Transaction objects on success, or a not found error.
     */
    fun retrieveTransactionsByMonthAndYear(token: String, month: Month, year: Int, account: String): Result<List<Transaction>>

    /**
     * Edit an existing transaction belonging to a specific account.
     *
     * @param accountID The UUID of the account containing the transaction.
     * @param transaction The Transaction object with updated values (must include id).
     * @param token Authentication token identifying the requester.
     * @return Result containing a TransactionResumeResult on success, or an error state on failure.
     */
    fun editTransaction(accountID: UUID, transaction: Transaction, token: String): Result<TransactionResumeResult>

    /**
     * Find a transaction by its unique identifier.
     *
     * @param id UUID of the transaction to find.
     * @param token Authentication token identifying the requester.
     * @return Result containing the Transaction on success, or TRANSACTION_NOT_FOUND on failure.
     */
    fun findById(id: UUID, token: String): Result<Transaction>

    /**
     * Delete multiple transaction sheets by their identifiers for a given account.
     *
     * @param accountID The UUID of the account which owns the transaction sheets.
     * @param sheetIds List of UUIDs corresponding to the transaction sheets to delete.
     * @param token Authentication token identifying the requester.
     * @return Result with no value on success, or an error state if the account or sheets are not found.
     */
    fun deleteSheetsByIds(accountID: UUID, sheetIds: List<UUID>, token: String): Result<TransactionDeletionResult>

    /**
     * Confirm a provisional (preview) transaction, converting it into a real transaction.
     *
     * @param token Authentication token identifying the requester.
     * @param accountID UUID of the account containing the preview transaction.
     * @param transactionId UUID of the preview transaction to confirm.
     * @return Result containing a TransactionResumeResult on success, or an appropriate failure state.
     */
    fun confirmPreviewTransaction(token: String, accountID: UUID, transactionId: UUID, newAmount: Amount?): Result<TransactionResumeResult>
}

@DomainService
class TransactionFeatureImpl(
    private val transactionRepository: TransactionRepository,
    private val session: SessionManager,
    private val accountRepository: BookletRepository,
    private val infraTransactionManager: UnitOfWorkTransactionProvider,
    private val tagRepository: TagRepository,
    private val trackerRepository: RegularTransactionTrackerRepository
): TransactionFeature{
    companion object {
        private val logger = Logger.getLogger(TransactionFeatureImpl::class.java.name)
    }

    override fun editTransaction(
        accountID: UUID,
        transaction: Transaction,
        token: String
    ): Result<TransactionResumeResult> = session.authenticate(token, roleUser){
        return@authenticate infraTransactionManager.executeInTransaction(transaction) {
            if(transaction.id == null) return@executeInTransaction failure(ResultState.TRANSACTION_ENTRY_ERROR, "L'ID de la transaction est null")
            val registeredAccount = accountRepository.findAccountByIdWithTransactions(accountID) ?: return@executeInTransaction notFound("Le compte $accountID n'existe pas")
            val transactionFromDatabase = registeredAccount.findTransactionById(transaction.id)?.copy() ?: return@executeInTransaction notFound("Aucune transaction n'existe avec l'ID suivant : ${transaction.id}")
            transactionFromDatabase.updateFromOther(transaction)
            transactionFromDatabase.lastModified = LocalDateTime.now()
            transactionRepository.save(registeredAccount.id!!, transactionFromDatabase) ?: return@executeInTransaction invalid("Une erreur est survenue lors de la mise à jour de la transaction ${transactionFromDatabase.id}")
            registeredAccount.removeTransactionById(transaction.id)
            registeredAccount.addTransaction(transactionFromDatabase)
            accountRepository.update(registeredAccount)
            success(TransactionResumeResult(transactionFromDatabase, registeredAccount.amount, registeredAccount.previewAmount))
        }
    }

    override fun bookTransaction(
        token: String,
        accountLabel: String,
        transaction: Transaction
    ): Result<TransactionResumeResult> = session.authenticate(token) { id ->
        return@authenticate infraTransactionManager.executeInTransaction(transaction) {
            logger.info("Request for a transaction with id $id")
            val account = accountRepository.findAccountByLabelWithTransactions(id, accountLabel)
                ?: return@executeInTransaction failure(ResultState.TRANSACTION_NOT_FOUND, "Le compte $accountLabel n'existe pas")
            val newTr =  transactionRepository.save(account.id!!, transaction)
                ?: return@executeInTransaction failure(ResultState.INFRASTRUCTURE_ERROR, "Erreur est survenu lors de la transaction")
            if(transaction.amount.isNegative()) {
                return@executeInTransaction failure(ResultState.TRANSACTION_ENTRY_ERROR, "Le montant de la transaction ne peut pas être négatif")
            }
            val toSaveTransaction = if (newTr.tag == null) {
                newTr.copy(
                    tag = tagRepository.defaultTag()
                )
            } else newTr
            account.addTransaction(toSaveTransaction)
            accountRepository.update(account)
            logger.info("Transaction $newTr has been created, the booklet sold has been updated : $account")
            success(TransactionResumeResult(newTr, account.amount, account.previewAmount))
        }
    }

    override fun retrieveTransactionsByMonthAndYear(
        token: String,
        month: Month,
        year: Int,
        account: String
    ): Result<List<Transaction>> = session.authenticate(token) {
        success(transactionRepository.findAccountWithSheetByLabelAndUser(account, it)?.retrieveSheetSurroundAndSortedByDate(month, year)
            ?: return@authenticate notFound("Aucun compte ne correspond au label indiqué")
        )
    }

    override fun findById(
        id: UUID,
        token: String
    ): Result<Transaction> = session.authenticate(token, roleUser) {
        logger.info("Request for a transaction with id $id")
        val sheet = transactionRepository.findTransactionById(id) ?: return@authenticate failure(ResultState.TRANSACTION_NOT_FOUND, "La transaction $id n'existe pas")
        success(sheet)
    }

    override fun deleteSheetsByIds(accountID: UUID, sheetIds: List<UUID>, token: String): Result<TransactionDeletionResult> {
        return infraTransactionManager.executeInTransaction(transactionRepository) {
            val booklet: Booklet = accountRepository.findAccountByIdWithTransactions(accountID)
                ?: return@executeInTransaction failure(ResultState.BOOKLET_NOT_FOUND, "Account $accountID n'existe pas")

            // Find transactions to delete and mark months as excluded if they are confirmed regular transactions
            val transactionsToDelete = booklet.transactions.filter { sheetIds.contains(it.id) }

            transactionsToDelete.forEach { transaction ->
                // If the transaction has a regularTransactionId, mark this month as excluded to prevent regeneration
                if (transaction.regularTransactionId != null) {
                    trackerRepository.markMonthAsExcluded(
                        regularTransactionId = transaction.regularTransactionId,
                        bookletId = accountID,
                        year = transaction.date.year,
                        month = transaction.date.month
                    )
                    logger.info("Marked month ${transaction.date.month}/${transaction.date.year} as excluded for regular transaction ${transaction.regularTransactionId}")
                }
            }

            booklet.removeTransactionIf { sheetIds.contains(it.id) }
            logger.info("Removed transactions with ids ${booklet.transactions}")
            transactionRepository.deleteAllSheetsById(sheetIds)
            // Use update (amount/previewAmount columns only) instead of upsert (full JPA save)
            // to avoid re-persisting all sheets which can create duplicates.
            accountRepository.update(booklet)
            logger.info("Deleted transactions with ids $sheetIds from account $accountID. Updated booklet: $booklet")
            return@executeInTransaction success(
                TransactionDeletionResult(
                    deletedIds = sheetIds,
                    accountAmount = booklet.amount,
                    accountPreviewAmount = booklet.previewAmount
                )
            )
        }
    }

    override fun confirmPreviewTransaction(
        token: String,
        accountID: UUID,
        transactionId: UUID,
        newAmount: Amount?
    ): Result<TransactionResumeResult> = session.authenticate(token) {
        return@authenticate infraTransactionManager.executeInTransaction(Any()) {
            val account = accountRepository.findAccountByIdWithTransactions(accountID)
                ?: return@executeInTransaction failure(ResultState.BOOKLET_NOT_FOUND, "Booklet $accountID not found")
            val transaction = transactionRepository.findTransactionById(transactionId)
                ?: return@executeInTransaction failure(ResultState.BOOKLET_NOT_FOUND, "Transaction not found")

            // IMPORTANT: removeTransactionById must be called BEFORE mutating transaction.isPreview.
            // The transaction object may be the same reference as the one held inside account.transactions
            // (depending on the repository implementation). If isPreview is set to false before the removal,
            // removeTransactionById would see it as a real (non-preview) transaction and incorrectly
            // subtract its amount from booklet.amount (the real balance), corrupting both balances.
            account.removeTransactionById(transactionId)

            // Build the confirmed copy with the (optionally updated) amount and isPreview = false.
            val confirmedTransaction = transaction.copy(
                amount = newAmount ?: transaction.amount,
                isPreview = false
            )
            transactionRepository.save(accountID, confirmedTransaction)
            account.addTransaction(confirmedTransaction)
            accountRepository.update(account)
            return@executeInTransaction success(TransactionResumeResult(confirmedTransaction, account.amount, account.previewAmount))
        }
    }

}

data class TransactionDeletionResult(
    val deletedIds: List<UUID>,
    val accountAmount: Amount,
    val accountPreviewAmount: Amount
)
