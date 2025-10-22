package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class PrevisionalTransactionFilterTest {

    private lateinit var filter: PrevisionalTransactionFilter

    @BeforeEach
    fun setup() {
        filter = PrevisionalTransactionFilterImpl()
    }

    @Test
    fun `filterPrevisionalTransactions should return empty result when no booklets`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(1)

        val result = filter.filterPrevisionalTransactions(emptyList(), startDate, endDate)

        assertTrue(result.transactions.isEmpty())
        assertTrue(result.groupedByAccount.isEmpty())
        assertEquals(0.toAmount(), result.totalAmount)
        assertEquals(0.toAmount(), result.totalIncome)
        assertEquals(0.toAmount(), result.totalExpenses)
    }

    @Test
    fun `filterPrevisionalTransactions should only include preview transactions`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(1)
        val booklet = Booklet(1000.toAmount(), "Account")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Regular", startDate.plusDays(5), 100.toAmount(), true, isPreview = false)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Preview", startDate.plusDays(10), 200.toAmount(), true, isPreview = true)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet), startDate, endDate)

        assertEquals(1, result.transactions.size)
        assertEquals("Preview", result.transactions[0].label)
    }

    @Test
    fun `filterPrevisionalTransactions should filter by date range`() {
        val startDate = LocalDate.of(2024, 6, 1)
        val endDate = LocalDate.of(2024, 6, 30)
        val booklet = Booklet(1000.toAmount(), "Account")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Before", LocalDate.of(2024, 5, 31), 100.toAmount(), true, isPreview = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "During", LocalDate.of(2024, 6, 15), 200.toAmount(), true, isPreview = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "After", LocalDate.of(2024, 7, 1), 300.toAmount(), true, isPreview = true)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet), startDate, endDate)

        assertEquals(1, result.transactions.size)
        assertEquals("During", result.transactions[0].label)
    }

    @Test
    fun `filterPrevisionalTransactions should include transactions on start and end dates`() {
        val startDate = LocalDate.of(2024, 6, 1)
        val endDate = LocalDate.of(2024, 6, 30)
        val booklet = Booklet(1000.toAmount(), "Account")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "On start", startDate, 100.toAmount(), true, isPreview = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "On end", endDate, 200.toAmount(), true, isPreview = true)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet), startDate, endDate)

        assertEquals(2, result.transactions.size)
    }

    @Test
    fun `filterPrevisionalTransactions should sort transactions by date`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(1)
        val booklet = Booklet(1000.toAmount(), "Account")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Later", startDate.plusDays(20), 100.toAmount(), true, isPreview = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Earlier", startDate.plusDays(5), 200.toAmount(), true, isPreview = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Middle", startDate.plusDays(10), 300.toAmount(), true, isPreview = true)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet), startDate, endDate)

        assertEquals("Earlier", result.transactions[0].label)
        assertEquals("Middle", result.transactions[1].label)
        assertEquals("Later", result.transactions[2].label)
    }

    @Test
    fun `filterPrevisionalTransactions should calculate correct total income`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(1)
        val booklet = Booklet(1000.toAmount(), "Account")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income 1", startDate.plusDays(5), 500.toAmount(), true, isPreview = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income 2", startDate.plusDays(10), 300.toAmount(), true, isPreview = true)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet), startDate, endDate)

        assertEquals(800.toAmount(), result.totalIncome)
    }

    @Test
    fun `filterPrevisionalTransactions should calculate correct total expenses`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(1)
        val booklet = Booklet(1000.toAmount(), "Account")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Expense 1", startDate.plusDays(5), 200.toAmount(), false, isPreview = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Expense 2", startDate.plusDays(10), 150.toAmount(), false, isPreview = true)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet), startDate, endDate)

        assertEquals(350.toAmount(), result.totalExpenses)
    }

    @Test
    fun `filterPrevisionalTransactions should calculate total amount as income minus expenses`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(1)
        val booklet = Booklet(1000.toAmount(), "Account")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income", startDate.plusDays(5), 1000.toAmount(), true, isPreview = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Expense", startDate.plusDays(10), 400.toAmount(), false, isPreview = true)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet), startDate, endDate)

        assertEquals(1000.toAmount(), result.totalIncome)
        assertEquals(400.toAmount(), result.totalExpenses)
        assertEquals(600.toAmount(), result.totalAmount)
    }

    @Test
    fun `filterPrevisionalTransactions should group transactions by account`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(1)
        val booklet1 = Booklet(1000.toAmount(), "Account 1")
        val booklet2 = Booklet(2000.toAmount(), "Account 2")

        booklet1.addTransaction(
            Transaction(UUID.randomUUID(), "Transaction 1", startDate.plusDays(5), 100.toAmount(), true, isPreview = true)
        )
        booklet2.addTransaction(
            Transaction(UUID.randomUUID(), "Transaction 2", startDate.plusDays(10), 200.toAmount(), true, isPreview = true)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet1, booklet2), startDate, endDate)

        assertEquals(2, result.groupedByAccount.size)
        assertTrue(result.groupedByAccount.containsKey("Account 1"))
        assertTrue(result.groupedByAccount.containsKey("Account 2"))
        assertEquals(1, result.groupedByAccount["Account 1"]!!.size)
        assertEquals(1, result.groupedByAccount["Account 2"]!!.size)
    }

    @Test
    fun `filterPrevisionalTransactions should exclude accounts with no preview transactions`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(1)
        val booklet1 = Booklet(1000.toAmount(), "With Preview")
        val booklet2 = Booklet(2000.toAmount(), "Without Preview")

        booklet1.addTransaction(
            Transaction(UUID.randomUUID(), "Preview", startDate.plusDays(5), 100.toAmount(), true, isPreview = true)
        )
        booklet2.addTransaction(
            Transaction(UUID.randomUUID(), "Regular", startDate.plusDays(10), 200.toAmount(), true, isPreview = false)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet1, booklet2), startDate, endDate)

        assertEquals(1, result.groupedByAccount.size)
        assertTrue(result.groupedByAccount.containsKey("With Preview"))
        assertFalse(result.groupedByAccount.containsKey("Without Preview"))
    }

    @Test
    fun `filterPrevisionalTransactions should sort grouped transactions by date`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(1)
        val booklet = Booklet(1000.toAmount(), "Account")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Later", startDate.plusDays(20), 100.toAmount(), true, isPreview = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Earlier", startDate.plusDays(5), 200.toAmount(), true, isPreview = true)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet), startDate, endDate)

        val accountTransactions = result.groupedByAccount["Account"]!!
        assertEquals("Earlier", accountTransactions[0].label)
        assertEquals("Later", accountTransactions[1].label)
    }

    @Test
    fun `filterPrevisionalTransactions should aggregate from multiple booklets`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(1)
        val booklet1 = Booklet(1000.toAmount(), "Account 1")
        val booklet2 = Booklet(2000.toAmount(), "Account 2")

        booklet1.addTransaction(
            Transaction(UUID.randomUUID(), "Income 1", startDate.plusDays(5), 300.toAmount(), true, isPreview = true)
        )
        booklet2.addTransaction(
            Transaction(UUID.randomUUID(), "Income 2", startDate.plusDays(10), 700.toAmount(), true, isPreview = true)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet1, booklet2), startDate, endDate)

        assertEquals(2, result.transactions.size)
        assertEquals(1000.toAmount(), result.totalIncome)
    }

    @Test
    fun `filterPrevisionalTransactions should handle mixed income and expenses`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(1)
        val booklet = Booklet(1000.toAmount(), "Account")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income", startDate.plusDays(5), 2000.toAmount(), true, isPreview = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Expense 1", startDate.plusDays(10), 500.toAmount(), false, isPreview = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Expense 2", startDate.plusDays(15), 300.toAmount(), false, isPreview = true)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet), startDate, endDate)

        assertEquals(3, result.transactions.size)
        assertEquals(2000.toAmount(), result.totalIncome)
        assertEquals(800.toAmount(), result.totalExpenses)
        assertEquals(1200.toAmount(), result.totalAmount)
    }

    @Test
    fun `filterPrevisionalTransactions should handle empty date range correctly`() {
        val date = LocalDate.now()
        val booklet = Booklet(1000.toAmount(), "Account")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Same day", date, 100.toAmount(), true, isPreview = true)
        )

        val result = filter.filterPrevisionalTransactions(listOf(booklet), date, date)

        assertEquals(1, result.transactions.size)
        assertEquals(100.toAmount(), result.totalIncome)
    }
}

