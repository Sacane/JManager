package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.repository.TransactionQueryRepository
import java.time.LocalDate
import java.util.UUID

class InMemoryTransactionQueryRepository(
    private val inMemoryDatabase: InMemoryDatabase
) : TransactionQueryRepository {

    override fun findByBookletIdAndDateBetween(
        bookletId: UUID,
        from: LocalDate,
        to: LocalDate
    ): List<Transaction> {
        val booklet = inMemoryDatabase.findAccountById(bookletId) ?: return emptyList()
        // mimic DB ordering: date asc then lastModified asc
        return booklet.transactions
            .filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
            .sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
    }
}

