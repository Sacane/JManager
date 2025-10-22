package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import fr.sacane.jmanager.domain.port.spi.RegularTransactionTrackerRepository
import java.util.UUID

class InMemoryRegularTrackerRepository(
    private val inMemoryDatabase: InMemoryDatabase
): RegularTransactionTrackerRepository {
    override fun findTracker(
        regularTransactionId: RegularTransactionId,
        bookletId: UUID
    ): RegularTransactionTracker? {
        return inMemoryDatabase.findTrackerByBookletAndTransaction(bookletId, regularTransactionId)
    }

    override fun upsertTracker(tracker: RegularTransactionTracker): RegularTransactionTracker {
        inMemoryDatabase.addTrackerByBooklet(tracker.bookletId, tracker)
        return tracker
    }

    override fun findAllTrackersForBooklet(bookletId: UUID): List<RegularTransactionTracker> {
        return inMemoryDatabase.findTrackerByBooklet(bookletId) ?: emptyList()
    }

    override fun deleteTrackerByBookletId(bookletId: UUID) {
        inMemoryDatabase.deleteTrackerByBookletId(bookletId)
    }

}