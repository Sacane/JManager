package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import java.time.Month
import java.time.YearMonth
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

    override fun deleteTrackerByRegularTransactionId(regularTransactionId: RegularTransactionId) {
        inMemoryDatabase.deleteTrackerByRegularTransactionId(regularTransactionId)
    }

    override fun deleteTrackerByPair(regularTransactionId: RegularTransactionId, bookletId: UUID) {
        inMemoryDatabase.deleteTrackerByPair(regularTransactionId, bookletId)
    }

    override fun unmarkMonthAsExcluded(
        regularTransactionId: RegularTransactionId,
        bookletId: UUID,
        year: Int,
        month: Month
    ) {
        val tracker = findTracker(regularTransactionId, bookletId) ?: return
        val updatedTracker = tracker.copy(
            excludedMonths = tracker.excludedMonths - YearMonth.of(year, month)
        )
        upsertTracker(updatedTracker)
    }

    override fun markMonthAsExcluded(
        regularTransactionId: RegularTransactionId,
        bookletId: UUID,
        year: Int,
        month: Month
    ) {
        val tracker = findTracker(regularTransactionId, bookletId)
        if (tracker != null) {
            val updatedTracker = tracker.copy(
                excludedMonths = tracker.excludedMonths + YearMonth.of(year, month)
            )
            upsertTracker(updatedTracker)
        } else {
            // If no tracker exists, create one with the excluded month
            val newTracker = RegularTransactionTracker(
                regularTransactionId = regularTransactionId,
                bookletId = bookletId,
                lastGeneratedDate = java.time.LocalDate.of(year, month, 1),
                numberOfGeneratedTransaction = 0,
                excludedMonths = setOf(YearMonth.of(year, month))
            )
            upsertTracker(newTracker)
        }
    }

}