package fr.sacane.jmanager.domain.port.spi.repository

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.time.LocalDate
import java.util.UUID

/**
 * Read-optimized queries for transactions.
 *
 * This port exists to keep the domain/service layer independent from the persistence
 * details while still allowing efficient DB-side filtering/sorting for large datasets.
 */
@Port(Side.INFRASTRUCTURE)
interface TransactionQueryRepository {
    /**
     * Returns all transactions for a given booklet within the inclusive [from]..[to] date range.
     * The result is expected to be ordered in a stable way for UI listing.
     */
    fun findByBookletIdAndDateBetween(
        bookletId: UUID,
        from: LocalDate,
        to: LocalDate
    ): List<Transaction>
}

