package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.utils.*
import java.time.LocalDate
import java.time.Month
import java.util.UUID

@Deprecated("Use individual use case interfaces from domain.port.input.transaction instead")
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
    fun bookTransaction(token: SessionToken, bookletLabel: String, transaction: Transaction): Result<TransactionResumeResult>

    /**
     * Retrieve transactions for a specific month and year for the given booklet label.
     *
     * @param token Authentication token identifying the requester.
     * @param month The month to retrieve transactions for.
     * @param year The year to retrieve transactions for.
     * @param bookletLabel The label of the booklet to fetch transactions from.
     * @return Result containing the list of Transaction objects on success, or a not found error.
     */
    fun retrieveTransactionsByMonthAndYear(token: SessionToken, month: Month, year: Int, bookletLabel: String): Result<List<Transaction>>

    /**
     * Edit an existing transaction belonging to a specific booklet.
     *
     * @param bookletID The UUID of the booklet containing the transaction.
     * @param transaction The Transaction object with updated values (must include id).
     * @param token Authentication token identifying the requester.
     * @return Result containing a TransactionResumeResult on success, or an error state on failure.
     */
    fun editTransaction(bookletID: UUID, transaction: Transaction, token: SessionToken): Result<TransactionResumeResult>

    /**
     * Find a transaction by its unique identifier.
     *
     * @param id UUID of the transaction to find.
     * @param token Authentication token identifying the requester.
     * @return Result containing the Transaction on success, or TRANSACTION_NOT_FOUND on failure.
     */
    fun findById(id: UUID, token: SessionToken): Result<Transaction>

    /**
     * Delete multiple transactions by their identifiers for a given booklet.
     *
     * @param bookletID The UUID of the booklet which owns the transactions.
     * @param transactionIds List of UUIDs corresponding to the transactions to delete.
     * @param token Authentication token identifying the requester.
     * @return Result with no value on success, or an error state if the booklet or transactions are not found.
     */
    fun deleteTransactionsByIds(bookletID: UUID, transactionIds: List<UUID>, token: SessionToken): Result<TransactionDeletionResult>

    /**
     * Confirm a provisional (preview) transaction, converting it into a real transaction.
     *
     * @param token Authentication token identifying the requester.
     * @param bookletID UUID of the booklet containing the preview transaction.
     * @param transactionId UUID of the preview transaction to confirm.
     * @return Result containing a TransactionResumeResult on success, or an appropriate failure state.
     */
    fun confirmPreviewTransaction(
        token: SessionToken,
        bookletID: UUID,
        transactionId: UUID,
        newAmount: Amount?,
        newDate: LocalDate?
    ): Result<TransactionResumeResult>
}

data class TransactionDeletionResult(
    val deletedIds: List<UUID>,
    val bookletAmount: Amount,
)
