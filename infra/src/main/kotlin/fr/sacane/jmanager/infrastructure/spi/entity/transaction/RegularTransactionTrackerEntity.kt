package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "regular_transaction_tracker")
data class RegularTransactionTrackerEntity(

    @Column(name = "regular_transaction_id", nullable = false)
    val regularTransactionId: String,

    @Column(name = "booklet_id", nullable = false)
    val bookletId: UUID,

    @Column(name = "last_generated_date", nullable = false)
    val lastGeneratedDate: LocalDate,

    @Column(name = "number_of_generated_transaction", nullable = false)
    val numberOfGeneratedTransaction: Int = 0,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    fun toDomain(): RegularTransactionTracker = RegularTransactionTracker(
        id = id,
        regularTransactionId = RegularTransactionId(regularTransactionId),
        bookletId = bookletId,
        lastGeneratedDate = lastGeneratedDate,
        numberOfGeneratedTransaction = numberOfGeneratedTransaction
    )

    companion object {
        fun fromDomain(tracker: RegularTransactionTracker): RegularTransactionTrackerEntity =
            RegularTransactionTrackerEntity(
                id = tracker.id,
                regularTransactionId = tracker.regularTransactionId.value,
                bookletId = tracker.bookletId,
                lastGeneratedDate = tracker.lastGeneratedDate,
                numberOfGeneratedTransaction = tracker.numberOfGeneratedTransaction
            )
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
        return jpaRepository.save(entity).toDomain()
    }

    override fun findAllTrackersForBooklet(bookletId: UUID): List<RegularTransactionTracker> {
        return jpaRepository.findAllByBookletId(bookletId).map { it.toDomain() }
    }

    override fun deleteTrackerByBookletId(bookletId: UUID) {
        jpaRepository.deleteAllByBookletId(bookletId)
    }
}