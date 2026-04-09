package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class TrendCalculatorTest {

    private lateinit var calculator: TrendCalculator

    @BeforeEach
    fun setup() {
        calculator = TrendCalculatorImpl()
    }

    @Test
    fun `calculateTrend should return 12 months of data`() {
        val booklets = listOf(
            Booklet(1000.toAmount(), "Booklet")
        )

        val trends = calculator.calculateTrend(booklets)

        assertEquals(12, trends.size)
    }

    @Test
    fun `calculateTrend should return empty amounts when no transactions`() {
        val booklets = listOf(
            Booklet(1000.toAmount(), "Empty Booklet")
        )

        val trends = calculator.calculateTrend(booklets)

        trends.forEach { trend ->
            assertEquals(0.toAmount(), trend.income)
            assertEquals(0.toAmount(), trend.expenses)
            assertEquals(0.toAmount(), trend.balance)
        }
    }

    @Test
    fun `calculateTrend should ignore preview transactions`() {
        val currentDate = LocalDate.now()
        val booklet = Booklet(1000.toAmount(), "Booklet")

        booklet.addTransaction(
            Transaction(
                UUID.randomUUID(), "Preview income", currentDate, 100.toAmount(),
                isIncome = true, isPreview = true
            )
        )
        booklet.addTransaction(
            Transaction(
                UUID.randomUUID(), "Preview expense", currentDate, 50.toAmount(),
                isIncome = false, isPreview = true
            )
        )

        val trends = calculator.calculateTrend(listOf(booklet))

        val currentMonthTrend = trends.find {
            it.month == currentDate.monthValue && it.year == currentDate.year
        }
        assertNotNull(currentMonthTrend)
        assertEquals(0.toAmount(), currentMonthTrend!!.income)
        assertEquals(0.toAmount(), currentMonthTrend.expenses)
    }

    @Test
    fun `calculateTrend should calculate correct income and expenses for current month`() {
        val currentDate = LocalDate.now()
        val booklet = Booklet(1000.toAmount(), "Booklet")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income", currentDate, 500.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Expense", currentDate, 200.toAmount(), isIncome = false)
        )

        val trends = calculator.calculateTrend(listOf(booklet))

        val currentMonthTrend = trends.find {
            it.month == currentDate.monthValue && it.year == currentDate.year
        }
        assertNotNull(currentMonthTrend)
        assertEquals(500.toAmount(), currentMonthTrend!!.income)
        assertEquals(200.toAmount(), currentMonthTrend.expenses)
        assertEquals(300.toAmount(), currentMonthTrend.balance)
    }

    @Test
    fun `calculateTrend should accumulate balance correctly across months`() {
        val currentDate = LocalDate.now()
        val lastMonth = currentDate.minusMonths(1)
        val booklet = Booklet(1000.toAmount(), "Booklet")


        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Last month income", lastMonth, 200.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Last month expense", lastMonth, 100.toAmount(), isIncome = false)
        )

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Current income", currentDate, 150.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Current expense", currentDate, 100.toAmount(), isIncome = false)
        )

        val trends = calculator.calculateTrend(listOf(booklet))

        val lastMonthTrend = trends.find {
            it.month == lastMonth.monthValue && it.year == lastMonth.year
        }
        val currentMonthTrend = trends.find {
            it.month == currentDate.monthValue && it.year == currentDate.year
        }

        assertNotNull(lastMonthTrend)
        assertNotNull(currentMonthTrend)

        assertEquals(100.toAmount(), lastMonthTrend!!.balance)
        assertEquals(50.toAmount(), currentMonthTrend!!.balance)

        assertTrue(currentMonthTrend.cumulativeBalance.value >= lastMonthTrend.cumulativeBalance.value)
    }

    @Test
    fun `calculateTrend should aggregate transactions from multiple booklets`() {
        val currentDate = LocalDate.now()
        val booklet1 = Booklet(1000.toAmount(), "Booklet 1")
        val booklet2 = Booklet(2000.toAmount(), "Booklet 2")

        booklet1.addTransaction(
            Transaction(UUID.randomUUID(), "Income 1", currentDate, 300.toAmount(), isIncome = true)
        )
        booklet2.addTransaction(
            Transaction(UUID.randomUUID(), "Income 2", currentDate, 200.toAmount(), isIncome = true)
        )

        val trends = calculator.calculateTrend(listOf(booklet1, booklet2))

        val currentMonthTrend = trends.find {
            it.month == currentDate.monthValue && it.year == currentDate.year
        }
        assertNotNull(currentMonthTrend)
        assertEquals(500.toAmount(), currentMonthTrend!!.income)
        assertEquals(2, currentMonthTrend.totalBooklets)
    }

    @Test
    fun `calculateTrend should set correct totalBooklets`() {
        val booklets = listOf(
            Booklet(1000.toAmount(), "Booklet 1"),
            Booklet(2000.toAmount(), "Booklet 2"),
            Booklet(3000.toAmount(), "Booklet 3")
        )

        val trends = calculator.calculateTrend(booklets)

        trends.forEach { trend ->
            assertEquals(3, trend.totalBooklets)
        }
    }

    @Test
    fun `calculateTrend should cover last 12 months including current month`() {
        val booklets = listOf(Booklet(1000.toAmount(), "Booklet"))
        val trends = calculator.calculateTrend(booklets)
        val currentDate = LocalDate.now()

        val currentMonthTrend = trends.find {
            it.month == currentDate.monthValue && it.year == currentDate.year
        }
        assertNotNull(currentMonthTrend)

        val elevenMonthsAgo = currentDate.minusMonths(11)
        val elevenMonthsAgoTrend = trends.find {
            it.month == elevenMonthsAgo.monthValue && it.year == elevenMonthsAgo.year
        }
        assertNotNull(elevenMonthsAgoTrend)
    }

    @Test
    fun `calculateTrend should calculate correct balance as income minus expenses`() {
        val currentDate = LocalDate.now()
        val booklet = Booklet(1000.toAmount(), "Booklet")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income", currentDate, 1000.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Expense", currentDate, 300.toAmount(), isIncome = false)
        )

        val trends = calculator.calculateTrend(listOf(booklet))

        val currentMonthTrend = trends.find {
            it.month == currentDate.monthValue && it.year == currentDate.year
        }
        assertNotNull(currentMonthTrend)

        val expectedBalance = currentMonthTrend!!.income.value.subtract(currentMonthTrend.expenses.value)
        assertEquals(expectedBalance, currentMonthTrend.balance.value)
    }

    @Test
    fun `calculateTrend should handle months without transactions`() {
        val twoMonthsAgo = LocalDate.now().minusMonths(2)
        val booklet = Booklet(1000.toAmount(), "Booklet")

        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Old income", twoMonthsAgo, 500.toAmount(), isIncome = true)
        )

        val trends = calculator.calculateTrend(listOf(booklet))

        val lastMonthTrend = trends.find {
            val lastMonth = LocalDate.now().minusMonths(1)
            it.month == lastMonth.monthValue && it.year == lastMonth.year
        }
        assertNotNull(lastMonthTrend)
        assertEquals(0.toAmount(), lastMonthTrend!!.income)
        assertEquals(0.toAmount(), lastMonthTrend.expenses)
        assertEquals(0.toAmount(), lastMonthTrend.balance)
    }

    @Test
    fun `calculateTrend should exclude transactions before startDate within the same calendar month`() {
        // Bug scenario: custom cycle starting on the 28th.
        // Transactions on Feb 1-27 must NOT be counted when startDate = Feb-28.
        val startDate = LocalDate.of(2026, 2, 28)
        val endDate   = LocalDate.of(2026, 3, 27)
        val booklet   = Booklet(1000.toAmount(), "Booklet")

        // This transaction is in February but BEFORE startDate — must be excluded
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Before start", LocalDate.of(2026, 2, 10), 300.toAmount(), isIncome = false)
        )
        // This transaction is on startDate — must be included
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "On start", LocalDate.of(2026, 2, 28), 100.toAmount(), isIncome = false)
        )

        val trends = calculator.calculateTrend(listOf(booklet), startDate, endDate)

        val febTrend = trends.find { it.month == 2 && it.year == 2026 }
        assertNotNull(febTrend)
        assertEquals(100.toAmount(), febTrend!!.expenses)
    }

    @Test
    fun `calculateTrend should exclude transactions after endDate within the same calendar month`() {
        // Bug scenario: custom cycle ending on the 27th.
        // Transactions on Mar 28-31 must NOT be counted when endDate = Mar-27.
        val startDate = LocalDate.of(2026, 2, 28)
        val endDate   = LocalDate.of(2026, 3, 27)
        val booklet   = Booklet(1000.toAmount(), "Booklet")

        // This transaction is on endDate — must be included
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "On end", LocalDate.of(2026, 3, 27), 150.toAmount(), isIncome = true)
        )
        // This transaction is in March but AFTER endDate — must be excluded
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "After end", LocalDate.of(2026, 3, 31), 400.toAmount(), isIncome = true)
        )

        val trends = calculator.calculateTrend(listOf(booklet), startDate, endDate)

        val marchTrend = trends.find { it.month == 3 && it.year == 2026 }
        assertNotNull(marchTrend)
        assertEquals(150.toAmount(), marchTrend!!.income)
    }
}

