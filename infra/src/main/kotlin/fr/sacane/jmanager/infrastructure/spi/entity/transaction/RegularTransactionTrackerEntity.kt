package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import fr.sacane.jmanager.domain.port.spi.RegularTransactionTrackerRepository
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Entity
@Table(name = "regular_transaction_tracker")
data class RegularTransactionTrackerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "regular_transaction_id", nullable = false)
    val regularTransactionId: String,

    @Column(name = "booklet_id", nullable = false)
    val bookletId: Long,

    @Column(name = "last_generated_date", nullable = false)
    val lastGeneratedDate: LocalDate,

) {
    fun toDomain(): RegularTransactionTracker = RegularTransactionTracker(
        id = id,
        regularTransactionId = RegularTransactionId(regularTransactionId),
        bookletId = bookletId,
        lastGeneratedDate = lastGeneratedDate
    )

    companion object {
        fun fromDomain(tracker: RegularTransactionTracker): RegularTransactionTrackerEntity =
            RegularTransactionTrackerEntity(
                id = tracker.id,
                regularTransactionId = tracker.regularTransactionId.value,
                bookletId = tracker.bookletId,
                lastGeneratedDate = tracker.lastGeneratedDate
            )
    }
}

@Repository
interface JpaRegularTransactionTrackerRepository : JpaRepository<RegularTransactionTrackerEntity, Long> {

    @Query("SELECT r FROM RegularTransactionTrackerEntity r WHERE r.regularTransactionId = :regularTransactionId AND r.bookletId = :bookletId")
    fun findByTransactionTrackerByRegularTransactionAndBookletId(regularTransactionId: String, bookletId: Long): RegularTransactionTrackerEntity?
    fun findAllByBookletId(bookletId: Long): List<RegularTransactionTrackerEntity>
    fun deleteAllByBookletId(bookletId: Long)
}

@Service
class RegularTransactionTrackerRepositoryAdapter(
    private val jpaRepository: JpaRegularTransactionTrackerRepository
) : RegularTransactionTrackerRepository {

    override fun findTracker(
        regularTransactionId: RegularTransactionId,
        bookletId: Long
    ): RegularTransactionTracker? {
        return jpaRepository.findByTransactionTrackerByRegularTransactionAndBookletId(regularTransactionId.value, bookletId)?.toDomain()
    }

    override fun upsertTracker(tracker: RegularTransactionTracker): RegularTransactionTracker {
        val entity = RegularTransactionTrackerEntity.fromDomain(tracker)
        return jpaRepository.save(entity).toDomain()
    }

    override fun findAllTrackersForBooklet(bookletId: Long): List<RegularTransactionTracker> {
        return jpaRepository.findAllByBookletId(bookletId).map { it.toDomain() }
    }

    override fun deleteTrackerByBookletId(bookletId: Long) {
        jpaRepository.deleteAllByBookletId(bookletId)
    }
}