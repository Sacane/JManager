package fr.sacane.jmanager.domain.port.spi.repository

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import java.util.UUID

/**
 * SPI contract for persistence and retrieval of regular (recurring) transactions.
 *
 * Implementations provide storage and retrieval operations for RegularTransaction aggregates
 * and allow the domain to generate, query and maintain recurring transaction definitions.
 */
interface RegularTransactionRepository {

    /**
     * Retrieve all regular transactions owned by the given user.
     *
     * @param userId Domain user identifier
     * @return List of RegularTransaction for the user (may be empty)
     */
    fun getAllRegularTransactions(userId: UserId): List<RegularTransaction>

    /**
     * Retrieve all regular transactions used by a specific account/booklet for the given user.
     *
     * @param userId Domain user identifier
     * @param accountID UUID of the booklet/account
     * @return List of RegularTransaction or null if none exist
     */
    fun getAllRegularUsedByAccount(userId: UserId, accountID: UUID): List<RegularTransaction>?

    /**
     * Persist a new regular transaction and associate it with a set of booklet identifiers.
     *
     * @param userId Domain user identifier
     * @param regularTransaction RegularTransaction to persist
     * @param bookletIds List of booklet UUIDs that will be associated with this regular transaction
     * @return Persisted RegularTransaction (with id assigned)
     */
    fun saveRegularTransaction(userId: UserId, regularTransaction: RegularTransaction, bookletIds: List<UUID>): RegularTransaction

    /**
     * Retrieve a regular transaction by its identifier for the given user.
     *
     * @param userId Domain user identifier
     * @param transactionId Identifier of the regular transaction
     * @return RegularTransaction or null if not found
     */
    fun getRegularTransactionById(userId: UserId, transactionId: RegularTransactionId): RegularTransaction?

    /**
     * Update an existing regular transaction for the user.
     *
     * @param userId Domain user identifier
     * @param regularTransaction RegularTransaction object with updated data (must contain id)
     * @return Updated RegularTransaction or null if the transaction does not exist
     */
    fun updateRegularTransaction(userId: UserId, regularTransaction: RegularTransaction): RegularTransaction?

    /**
     * Delete a regular transaction by id for the given user.
     *
     * @param userId Domain user identifier
     * @param transactionId Identifier of the regular transaction to delete
     * @return true if deletion succeeded, false otherwise
     */
    fun deleteRegularTransaction(userId: UserId, transactionId: RegularTransactionId): Boolean
}