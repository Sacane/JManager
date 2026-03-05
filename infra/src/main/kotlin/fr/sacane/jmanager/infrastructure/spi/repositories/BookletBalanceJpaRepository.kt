package fr.sacane.jmanager.infrastructure.spi.repositories

import fr.sacane.jmanager.infrastructure.spi.entity.BookletResource
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

/**
 * Read-only queries to fetch persisted balances without loading the whole aggregate.
 */
@Component
interface BookletBalanceJpaRepository : Repository<BookletResource, UUID> {

    @Query(
        """
        SELECT acc.label AS label, acc.amount AS amount
        FROM BookletResource acc
        WHERE acc.idAccount = :id
        """
    )
    fun findPersistedBalances(@Param("id") id: UUID): PersistedBalancesRow?
}

/**
 * Projection used by Spring Data.
 */
interface PersistedBalancesRow {
    val label: String
    val amount: BigDecimal
}
