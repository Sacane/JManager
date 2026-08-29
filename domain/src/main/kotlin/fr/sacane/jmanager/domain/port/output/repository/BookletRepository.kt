package fr.sacane.jmanager.domain.port.output.repository

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import java.util.UUID

@Port(Side.INFRASTRUCTURE)
/**
 * SPI contract for persistence and retrieval operations related to Booklet aggregates.
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
     * @param bookletId UUID of the booklet
     * @return Booklet with transactions or null if not found
     */
    fun findBookletByIdWithTransactions(bookletId: UUID): Booklet?

    /**
     * Retrieve a booklet by label for the given user, including transactions.
     *
     * @param userId Owner user id
     * @param bookletLabel Label of the booklet to find
     * @return Booklet with transactions or null if not found
     */
    fun findBookletByLabelWithTransactions(userId: UserId, bookletLabel: String): Booklet?

    /**
     * Delete a booklet by its UUID. Implementations should also handle any cascade
     * or cleanup semantics appropriate to the persistence layer.
     *
     * @param bookletId UUID of the booklet to delete
     */
    fun deleteBookletById(bookletId: UUID)

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
     * Update monthly cycle settings for a booklet.
     *
     * @param bookletId UUID of the booklet to update
     * @param monthlyPeriodStartDay configured period start day (1..31)
     * @param monthlyPeriodEndDay configured period end day (1..31) or null to keep default behavior
     * @return true when one row was updated, false otherwise
     */
    fun updateMonthlyPeriodStartDay(bookletId: UUID, monthlyPeriodStartDay: Int, monthlyPeriodEndDay: Int? = null): Boolean

    /**
     * Retrieve all booklets owned by a user.
     *
     * @param userId Domain user identifier
     * @return List of Booklet aggregates (may be empty)
     */
    fun findBookletsForUser(userId: UserId): List<Booklet>

    /**
     * Tells whether [bookletId] exists and is owned by [userId], without loading the booklet's
     * transactions or regular transactions.
     *
     * Used for ownership checks (see [fr.sacane.jmanager.domain.port.input.booklet.userOwnsBooklet]).
     * Implementations must resolve this cheaply — ownership checks run ahead of every mutating
     * booklet/transaction use case, so they must not pull in unrelated aggregate data.
     *
     * @param userId Domain user identifier
     * @param bookletId UUID of the booklet
     * @return true if the booklet exists and belongs to the user, false otherwise
     */
    fun existsBookletForUser(userId: UserId, bookletId: UUID): Boolean
}