package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.utils.Result
import java.util.UUID

@Deprecated("Use individual use case interfaces from domain.port.input.regularTransaction instead")
@Port(Side.APPLICATION)
/**
 * Application port: RegularTransactionFeature
 *
 * High-level API for managing regular (recurring) transactions exposed to the application layer.
 * Implementations are responsible for authentication and returning domain Result<T>
 * objects that represent success or failure states.
 */
sealed interface RegularTransactionFeature {

    /**
     * Retrieve all regular transactions for the authenticated user, paginated.
     *
     * @param token Authentication token identifying the requester.
     * @param pageNumber Zero-based page number (default: 0).
     * @param pageSize Number of items per page (default: 10).
     * @return Result containing a Page of RegularTransaction on success.
     */
    fun getAllRegularTransactions(token: SessionToken, pageNumber: Int = 0, pageSize: Int = 10): Result<Page<RegularTransaction>>

    /**
     * Create (book) a new regular transaction and associate it with multiple booklets.
     *
     * @param token Authentication token identifying the requester.
     * @param regularTransaction The RegularTransaction to persist.
     * @param bookletIds List of booklet UUIDs that will be associated with the created regular transaction.
     * @return Result containing the persisted RegularTransaction on success, or an error state.
     */
    fun bookRegularTransaction(
        token: SessionToken,
        regularTransaction: RegularTransaction,
        bookletIds: List<UUID>
    ): Result<RegularTransaction>

    /**
     * Retrieve a single regular transaction by its identifier.
     *
     * @param token Authentication token identifying the requester.
     * @param transactionId The identifier of the regular transaction to retrieve.
     * @return Result containing the RegularTransaction on success, or TRANSACTION_NOT_FOUND when missing.
     */
    fun getRegularTransactionById(token: SessionToken, transactionId: String): Result<RegularTransaction>

    /**
     * Update an existing regular transaction.
     *
     * @param token Authentication token identifying the requester.
     * @param regularTransaction RegularTransaction object containing updated values (must include id).
        * @param bookletIds Booklet identifiers to associate with this regular transaction.
     * @return Result containing the updated RegularTransaction on success, or an error state if not found.
     */
        fun updateRegularTransaction(token: SessionToken, regularTransaction: RegularTransaction, bookletIds: List<UUID>): Result<RegularTransaction>

    /**
     * Delete a regular transaction by its identifier.
     *
     * @param token Authentication token identifying the requester.
     * @param transactionId Identifier of the regular transaction to delete.
     * @return Result containing a boolean indicating deletion success, or a failure when not found.
     */
    fun deleteRegularTransaction(token: SessionToken, transactionId: String): Result<Boolean>

    /**
     * Delete multiple regular transactions in a single operation.
     *
     * @param token Authentication token identifying the requester.
     * @param transactionIds Identifiers of regular transactions to delete.
     * @return Result containing deleted transaction ids, or a failure when any id is missing.
     */
    fun deleteRegularTransactions(token: SessionToken, transactionIds: List<String>): Result<List<String>>

    /**
     * Link a booklet to an existing regular transaction.
     *
     * @param token Authentication token identifying the requester.
     * @param transactionId Identifier of the regular transaction.
     * @param bookletId UUID of the booklet to link.
     * @return Result containing the updated RegularTransaction on success, or an error state.
     */
    fun linkRegularTransactionToBooklet(token: SessionToken, transactionId: String, bookletId: UUID): Result<RegularTransaction>

    /**
     * Unlink a booklet from an existing regular transaction.
     * Removing the link also deletes the generation tracker for that pair,
     * so no more virtual/preview transactions will be generated for this booklet.
     *
     * @param token Authentication token identifying the requester.
     * @param transactionId Identifier of the regular transaction.
     * @param bookletId UUID of the booklet to unlink.
     * @return Result containing the updated RegularTransaction on success, or an error state.
     */
    fun unlinkRegularTransactionFromBooklet(token: SessionToken, transactionId: String, bookletId: UUID): Result<RegularTransaction>
}
