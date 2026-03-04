package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

@Entity
@Table(
    name = "regular_transaction_tracker",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_regular_transaction_tracker_regular_booklet",
            columnNames = ["regular_transaction_id", "booklet_id"]
        )
    ]
)
data class RegularTransactionTrackerEntity(

    @Column(name = "regular_transaction_id", nullable = false)
    val regularTransactionId: String,

    @Column(name = "booklet_id", nullable = false)
    val bookletId: UUID,

    @Column(name = "last_generated_date", nullable = false)
    val lastGeneratedDate: LocalDate,

    @Column(name = "number_of_generated_transaction", nullable = false)
    val numberOfGeneratedTransaction: Int = 0,

    @Column(name = "excluded_months", columnDefinition = "TEXT")
    val excludedMonths: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    fun toDomain(): RegularTransactionTracker = RegularTransactionTracker(
        id = id,
        regularTransactionId = RegularTransactionId(regularTransactionId),
        bookletId = bookletId,
        lastGeneratedDate = lastGeneratedDate,
        numberOfGeneratedTransaction = numberOfGeneratedTransaction,
        excludedMonths = parseExcludedMonths(excludedMonths)
    )

    companion object {
        fun fromDomain(tracker: RegularTransactionTracker): RegularTransactionTrackerEntity =
            RegularTransactionTrackerEntity(
                id = tracker.id,
                regularTransactionId = tracker.regularTransactionId.value,
                bookletId = tracker.bookletId,
                lastGeneratedDate = tracker.lastGeneratedDate,
                numberOfGeneratedTransaction = tracker.numberOfGeneratedTransaction,
                excludedMonths = serializeExcludedMonths(tracker.excludedMonths)
            )

        private fun serializeExcludedMonths(months: Set<YearMonth>): String? {
            if (months.isEmpty()) return null
            return months.joinToString(",") { it.toString() }
        }

        private fun parseExcludedMonths(serialized: String?): Set<YearMonth> {
            if (serialized.isNullOrBlank()) return emptySet()
            return serialized.split(",")
                .filter { it.isNotBlank() }
                .map { YearMonth.parse(it) }
                .toSet()
        }
    }
}

@Repository
interface JpaRegularTransactionTrackerRepository : JpaRepository<RegularTransactionTrackerEntity, UUID> {

    @Query("SELECT r FROM RegularTransactionTrackerEntity r WHERE r.regularTransactionId = :regularTransactionId AND r.bookletId = :bookletId")
    fun findByTransactionTrackerByRegularTransactionAndBookletId(regularTransactionId: String, bookletId: UUID): RegularTransactionTrackerEntity?
    fun findAllByBookletId(bookletId: UUID): List<RegularTransactionTrackerEntity>
    fun deleteAllByBookletId(bookletId: UUID)
}

@Service
class RegularTransactionTrackerRepositoryAdapter(
    private val jpaRepository: JpaRegularTransactionTrackerRepository
) : RegularTransactionTrackerRepository {

    override fun findTracker(
        regularTransactionId: RegularTransactionId,
        bookletId: UUID
    ): RegularTransactionTracker? {
        return jpaRepository.findByTransactionTrackerByRegularTransactionAndBookletId(regularTransactionId.value, bookletId)?.toDomain()
    }

    override fun upsertTracker(tracker: RegularTransactionTracker): RegularTransactionTracker {
        val entity = RegularTransactionTrackerEntity.fromDomain(tracker)
        return try {
            jpaRepository.save(entity).toDomain()
        } catch (_: DataIntegrityViolationException) {
            // Concurrent creation may hit the unique constraint; reload and update instead.
            val existing = jpaRepository.findByTransactionTrackerByRegularTransactionAndBookletId(
                tracker.regularTransactionId.value,
                tracker.bookletId
            )
            if (existing == null) {
                throw IllegalStateException("Tracker conflict detected but no existing row found")
            }
            jpaRepository.save(entity.copy(id = existing.id)).toDomain()
        }
    }

    override fun findAllTrackersForBooklet(bookletId: UUID): List<RegularTransactionTracker> {
        return jpaRepository.findAllByBookletId(bookletId).map { it.toDomain() }
    }

    override fun deleteTrackerByBookletId(bookletId: UUID) {
        jpaRepository.deleteAllByBookletId(bookletId)
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
                lastGeneratedDate = LocalDate.of(year, month, 1),
                numberOfGeneratedTransaction = 0,
                excludedMonths = setOf(YearMonth.of(year, month))
            )
            upsertTracker(newTracker)
        }
    }
}
