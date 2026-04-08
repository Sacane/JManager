package fr.sacane.jmanager.domain.port.spi.repository

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import java.time.Month
import java.util.UUID

@Port(Side.INFRASTRUCTURE)
interface RegularTransactionTrackerRepository {
    /**
     * Finds a RegularTransactionTracker based on the provided regular transaction ID and booklet ID.
     *
     * @param regularTransactionId The identifier of the regular transaction to find the corresponding tracker.
     * @param bookletId The identifier of the booklet associated with the tracker to be retrieved.
     * @return A RegularTransactionTracker object if found, or null if no tracker exists for the given IDs.
     */
    fun findTracker(regularTransactionId: RegularTransactionId, bookletId: UUID): RegularTransactionTracker?

    /**
     * Inserts the given tracker into the repository if it doesn't already exist,
     * or updates the existing tracker if it does.
     *
     * @param tracker The instance of RegularTransactionTracker that needs to be inserted or updated.
     * @return The updated or newly inserted RegularTransactionTracker instance.
     */
    fun upsertTracker(tracker: RegularTransactionTracker): RegularTransactionTracker

    /**
     * Retrieves all trackers associated with a specific booklet.
     *
     * @param bookletId The identifier of the booklet for which the trackers are retrieved.
     * @return A list of RegularTransactionTracker objects associated with the provided booklet ID.
     */
    fun findAllTrackersForBooklet(bookletId: UUID): List<RegularTransactionTracker>

    /**
     * Deletes all trackers associated with a specific booklet.
     *
     * @param bookletId The identifier of the booklet for which the trackers are deleted.
     */
    fun deleteTrackerByBookletId(bookletId: UUID)

    /**
     * Deletes all trackers associated with a specific regular transaction.
     *
     * @param regularTransactionId The identifier of the regular transaction for which trackers are deleted.
     */
    fun deleteTrackerByRegularTransactionId(regularTransactionId: RegularTransactionId)

    /**
     * Marks a specific month and year as excluded for a regular transaction in a booklet.
     * This prevents the transaction from being regenerated for that month.
     *
     * @param regularTransactionId The identifier of the regular transaction.
     * @param bookletId The identifier of the booklet.
     * @param year The year to exclude.
     * @param month The month to exclude.
     */
    fun markMonthAsExcluded(
        regularTransactionId: RegularTransactionId,
        bookletId: UUID,
        year: Int,
        month: Month
    )

    /**
     * Removes the exclusion mark for a specific month and year for a regular transaction in a booklet.
     * This allows the transaction to be regenerated for that month if it was previously excluded.
     *
     * @param regularTransactionId The identifier of the regular transaction.
     * @param bookletId The identifier of the booklet.
     * @param year The year to un-exclude.
     * @param month The month to un-exclude.
     */
    fun unmarkMonthAsExcluded(
        regularTransactionId: RegularTransactionId,
        bookletId: UUID,
        year: Int,
        month: Month
    )

    /**
     * Deletes the tracker for a specific regular transaction / booklet pair.
     * This must be called when unlinking a regular transaction from a booklet
     * to ensure no virtual transactions are generated for that pair anymore.
     *
     * @param regularTransactionId The identifier of the regular transaction.
     * @param bookletId The identifier of the booklet.
     */
    fun deleteTrackerByPair(regularTransactionId: RegularTransactionId, bookletId: UUID)
}