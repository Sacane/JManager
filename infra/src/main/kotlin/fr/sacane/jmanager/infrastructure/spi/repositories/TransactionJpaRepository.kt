package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.TransactionResource
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface TransactionJpaRepository : CrudRepository<TransactionResource, UUID> {
    fun findSheetResourceByIdSheet(id: UUID): TransactionResource?

    // Direct JPQL DELETE — bypasses the Hibernate first-level cache and CascadeType.ALL
    // on BookletResource.sheets, which would otherwise re-insert the deleted rows on flush.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM TransactionResource t WHERE t.idSheet IN :ids")
    @Transactional
    fun deleteByIdSheetIn(ids: List<UUID>)

    // Direct query by booklet + date range — avoids loading the full BookletResource aggregate
    // which causes NonUniqueResultException when multiple JOIN FETCH are active in the same session.
    @Query("""
        SELECT s FROM TransactionResource s
        LEFT JOIN FETCH s.personalTag
        LEFT JOIN FETCH s.tag
        WHERE s.account.idAccount = :bookletId
          AND s.date >= :from
          AND s.date <= :to
    """)
    fun findByBookletIdAndDateBetween(
        bookletId: UUID,
        from: LocalDate,
        to: LocalDate
    ): List<TransactionResource>
}
