package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.fake.InMemoryBookletRepository
import fr.sacane.jmanager.domain.fake.InMemoryTransactionRepository
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.csv.CsvLineResult
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

/**
 * Delegates every call to [delegate] while counting how many times [update] is invoked — used to
 * catch the N+1 documented in
 * docs/technical/jpa-transactions/2026-08-29-jpa-fetch-and-transaction-boundary-audit.md
 * (finding C): `saveTransactions` used to call `bookletRepository.update` once per successfully
 * imported CSV row instead of once for the whole import.
 */
private class CountingBookletRepository(private val delegate: BookletRepository) : BookletRepository by delegate {
    var updateCallCount: Int = 0
        private set

    override fun update(booklet: Booklet) {
        updateCallCount++
        delegate.update(booklet)
    }
}

class CsvDomainHelperTest {

    @Test
    fun `saveTransactions must update the booklet balance once, not once per imported row`() {
        val db = InMemoryDatabase()
        val transactionRepository = InMemoryTransactionRepository(db)
        val bookletRepository = CountingBookletRepository(InMemoryBookletRepository(db))

        val booklet = Booklet(id = UUID.randomUUID(), amount = 0.toAmount(), label = "csv-import-test")

        val results = (1..5).map { n ->
            CsvLineResult.Success(
                Transaction(
                    id = null,
                    label = "line-$n",
                    date = LocalDate.of(2024, 1, n),
                    amount = Amount(10L * n),
                    isIncome = false,
                    isPreview = false,
                    tag = null,
                )
            )
        }

        val result = saveTransactions(transactionRepository, bookletRepository, booklet, results)

        assertEquals(5, result.successCount)
        assertEquals(
            1,
            bookletRepository.updateCallCount,
            "bookletRepository.update must be called once for the whole import, not once per row"
        )
    }
}
