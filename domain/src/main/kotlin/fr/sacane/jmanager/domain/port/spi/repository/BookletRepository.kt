package fr.sacane.jmanager.domain.port.spi.repository

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import java.util.UUID

@Port(Side.INFRASTRUCTURE)
/**
 * SPI contract for persistence and retrieval operations related to Booklet (account) aggregates.
 *
 * Implementations provide concrete adapters (JPA, JDBC, in-memory, etc.) that persist and
 * retrieve Booklet aggregates used by the domain use-cases. The domain depends on this abstraction
 * so that business logic remains independent from any storage technology.
 */
interface BookletRepository {
    /**
     * Update a booklet from another instance (useful for mapping or partial updates).
     *
     * @param booklet Booklet containing updated values
     * @return Updated Booklet or null on failure
     */
    fun editFromAnother(booklet: Booklet): Booklet?

    /**
     * Persist a new booklet for the specified owner.
     *
     * @param ownerId Owner user id
     * @param booklet Booklet to persist
     * @return Persisted Booklet or null on failure
     */
    fun save(ownerId: UserId, booklet: Booklet): Booklet?

    /**
     * Retrieve a booklet with its transactions eagerly loaded by id.
     *
     * @param accountId UUID of the booklet
     * @return Booklet with transactions or null if not found
     */
    fun findAccountByIdWithTransactions(accountId: UUID): Booklet?

    /**
     * Retrieve a booklet by label for the given user, including transactions.
     *
     * @param userId Owner user id
     * @param accountLabel Label of the account to find
     * @return Booklet with transactions or null if not found
     */
    fun findAccountByLabelWithTransactions(userId: UserId, accountLabel: String): Booklet?

    /**
     * Delete an account by its UUID. Implementations should also handle any cascade
     * or cleanup semantics appropriate to the persistence layer.
     *
     * @param accountId UUID of the account to delete
     */
    fun deleteAccountById(accountId: UUID)

    /**
     * Insert or update a booklet aggregate.
     *
     * @param booklet Booklet to upsert
     * @return Persisted Booklet
     */
    fun upsert(booklet: Booklet): Booklet

    /**
     * Update a booklet record.
     *
     * @param booklet Booklet with updated values
     */
    fun update(booklet: Booklet)

    /**
     * Update only the monthly period start day setting for a booklet.
     *
     * @param accountId UUID of the account to update
     * @param monthlyPeriodStartDay configured period start day (1..31)
     * @return true when one row was updated, false otherwise
     */
    fun updateMonthlyPeriodStartDay(accountId: UUID, monthlyPeriodStartDay: Int): Boolean

    /**
     * Retrieve all booklets owned by a user.
     *
     * @param userId Domain user identifier
     * @return List of Booklet aggregates (may be empty)
     */
    fun findBookletsForUser(userId: UserId): List<Booklet>
}