package fr.sacane.jmanager.domain.port.spi.repository

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
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
}