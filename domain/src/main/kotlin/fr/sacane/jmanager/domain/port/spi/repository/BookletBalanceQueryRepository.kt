package fr.sacane.jmanager.domain.port.spi.repository

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import java.math.BigDecimal
import java.util.UUID

/**
 * Read-optimized access for booklet balances.
 *
 * This port exists to avoid loading the full Booklet aggregate (and its transactions)
 * when we only need persisted balances.
 */
@Port(Side.INFRASTRUCTURE)
interface BookletBalanceQueryRepository {
    data class PersistedBalances(
        val label: String,
        val amount: BigDecimal,
    )

    fun findPersistedBalances(bookletId: UUID): PersistedBalances?
}
