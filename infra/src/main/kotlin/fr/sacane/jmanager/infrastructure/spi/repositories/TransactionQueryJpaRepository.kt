package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.TransactionResource
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

/**
 * Read-only JPA queries for transaction listing.
 *
 * We keep it separate from TransactionJpaRepository (CrudRepository) to avoid mixing
 * write-oriented methods with potentially heavy read queries.
 */
@Component
interface TransactionQueryJpaRepository : Repository<TransactionResource, UUID> {

    @Query(
        """
        SELECT s
        FROM TransactionResource s
        LEFT JOIN FETCH s.personalTag
        LEFT JOIN FETCH s.tag
        WHERE s.account.idAccount = :bookletId
          AND s.date >= :from
          AND s.date <= :to
        ORDER BY s.date, s.lastModified
        """
    )
    fun findByBookletIdAndDateBetween(
        @Param("bookletId") bookletId: UUID,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate
    ): List<TransactionResource>
}

