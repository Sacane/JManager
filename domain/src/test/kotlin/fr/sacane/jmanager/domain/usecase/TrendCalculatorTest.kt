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
            Booklet(1000.toAmount(), "Account")
        )

        val trends = calculator.calculateTrend(booklets)

        assertEquals(12, trends.size)
    }

    @Test
    fun `calculateTrend should return empty amounts when no transactions`() {
        val booklets = listOf(
            Booklet(1000.toAmount(), "Empty Account")
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
        val booklet = Booklet(1000.toAmount(), "Account")

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
        val booklet = Booklet(1000.toAmount(), "Account")

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
        val booklet = Booklet(1000.toAmount(), "Account")


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
        val booklet1 = Booklet(1000.toAmount(), "Account 1")
        val booklet2 = Booklet(2000.toAmount(), "Account 2")

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
        assertEquals(2, currentMonthTrend.totalAccounts)
    }

    @Test
    fun `calculateTrend should set correct totalAccounts`() {
        val booklets = listOf(
            Booklet(1000.toAmount(), "Account 1"),
            Booklet(2000.toAmount(), "Account 2"),
            Booklet(3000.toAmount(), "Account 3")
        )

        val trends = calculator.calculateTrend(booklets)

        trends.forEach { trend ->
            assertEquals(3, trend.totalAccounts)
        }
    }

    @Test
    fun `calculateTrend should cover last 12 months including current month`() {
        val booklets = listOf(Booklet(1000.toAmount(), "Account"))
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
        val booklet = Booklet(1000.toAmount(), "Account")

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
        val booklet = Booklet(1000.toAmount(), "Account")

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
}

