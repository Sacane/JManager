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

class DailyTrendCalculatorTest {

    private lateinit var calculator: DailyTrendCalculator

    @BeforeEach
    fun setup() {
        calculator = DailyTrendCalculatorImpl()
    }

    @Test
    fun `should return one entry per day for a standard calendar month`() {
        val booklet = Booklet(1000.toAmount(), "Booklet")
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income", LocalDate.of(2025, 1, 5), 500.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Expense", LocalDate.of(2025, 1, 12), 200.toAmount(), isIncome = false)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income2", LocalDate.of(2025, 1, 20), 300.toAmount(), isIncome = true)
        )

        val trends = calculator.calculateDailyTrend(
            listOf(booklet),
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31)
        )

        assertEquals(31, trends.size)
        assertEquals(LocalDate.of(2025, 1, 1), trends.first().date)
        assertEquals(LocalDate.of(2025, 1, 31), trends.last().date)
    }

    @Test
    fun `should compute correct income and expenses per day`() {
        val booklet = Booklet(1000.toAmount(), "Booklet")
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income", LocalDate.of(2025, 1, 5), 500.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Expense", LocalDate.of(2025, 1, 5), 200.toAmount(), isIncome = false)
        )

        val trends = calculator.calculateDailyTrend(
            listOf(booklet),
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31)
        )

        val day5 = trends.find { it.date == LocalDate.of(2025, 1, 5) }!!
        assertEquals(500.toAmount(), day5.income)
        assertEquals(200.toAmount(), day5.expenses)
        assertEquals(300.toAmount(), day5.balance)
    }

    @Test
    fun `should return zero amounts for days without transactions`() {
        val booklet = Booklet(1000.toAmount(), "Booklet")
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income", LocalDate.of(2025, 1, 10), 100.toAmount(), isIncome = true)
        )

        val trends = calculator.calculateDailyTrend(
            listOf(booklet),
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31)
        )

        val day1 = trends.find { it.date == LocalDate.of(2025, 1, 1) }!!
        assertEquals(0.toAmount(), day1.income)
        assertEquals(0.toAmount(), day1.expenses)
        assertEquals(0.toAmount(), day1.balance)
    }

    @Test
    fun `should compute cumulative balance sequentially across the range`() {
        val booklet = Booklet(1000.toAmount(), "Booklet")
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income", LocalDate.of(2025, 1, 1), 1000.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Expense", LocalDate.of(2025, 1, 10), 300.toAmount(), isIncome = false)
        )

        val trends = calculator.calculateDailyTrend(
            listOf(booklet),
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 30)
        )

        val day1 = trends.find { it.date == LocalDate.of(2025, 1, 1) }!!
        val day10 = trends.find { it.date == LocalDate.of(2025, 1, 10) }!!
        val day30 = trends.find { it.date == LocalDate.of(2025, 1, 30) }!!

        assertEquals(BigDecimal("1000.00"), day1.cumulativeBalance.value)
        assertEquals(BigDecimal("700.00"), day10.cumulativeBalance.value)
        assertEquals(BigDecimal("700.00"), day30.cumulativeBalance.value)
    }

    @Test
    fun `should handle custom monthly cycle crossing month boundary`() {
        val booklet = Booklet(1000.toAmount(), "Booklet")
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Dec income", LocalDate.of(2024, 12, 28), 400.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Jan expense", LocalDate.of(2025, 1, 15), 150.toAmount(), isIncome = false)
        )

        val trends = calculator.calculateDailyTrend(
            listOf(booklet),
            LocalDate.of(2024, 12, 26),
            LocalDate.of(2025, 1, 25)
        )

        assertEquals(31, trends.size)
        assertEquals(LocalDate.of(2024, 12, 26), trends.first().date)
        assertEquals(LocalDate.of(2025, 1, 25), trends.last().date)

        val dec28 = trends.find { it.date == LocalDate.of(2024, 12, 28) }!!
        assertEquals(400.toAmount(), dec28.income)

        val jan15 = trends.find { it.date == LocalDate.of(2025, 1, 15) }!!
        assertEquals(150.toAmount(), jan15.expenses)

        assertEquals(BigDecimal("250.00"), trends.last().cumulativeBalance.value)
    }

    @Test
    fun `should handle cross-month custom cycle 25th to 24th for June`() {
        val booklet = Booklet(1000.toAmount(), "Booklet")
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "May expense", LocalDate.of(2025, 5, 27), 100.toAmount(), isIncome = false)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Jun income", LocalDate.of(2025, 6, 3), 500.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Jun expense", LocalDate.of(2025, 6, 18), 200.toAmount(), isIncome = false)
        )

        val trends = calculator.calculateDailyTrend(
            listOf(booklet),
            LocalDate.of(2025, 5, 25),
            LocalDate.of(2025, 6, 24)
        )

        assertEquals(31, trends.size)
        assertEquals(LocalDate.of(2025, 5, 25), trends.first().date)
        assertEquals(LocalDate.of(2025, 6, 24), trends.last().date)

        val may27 = trends.find { it.date == LocalDate.of(2025, 5, 27) }!!
        assertEquals(100.toAmount(), may27.expenses)

        val jun3 = trends.find { it.date == LocalDate.of(2025, 6, 3) }!!
        assertEquals(500.toAmount(), jun3.income)

        val jun18 = trends.find { it.date == LocalDate.of(2025, 6, 18) }!!
        assertEquals(200.toAmount(), jun18.expenses)

        assertEquals(BigDecimal("200.00"), trends.last().cumulativeBalance.value)
    }

    @Test
    fun `should exclude preview transactions`() {
        val booklet = Booklet(1000.toAmount(), "Booklet")
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Real", LocalDate.of(2025, 1, 5), 100.toAmount(), isIncome = true, isPreview = false)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Preview", LocalDate.of(2025, 1, 5), 999.toAmount(), isIncome = true, isPreview = true)
        )

        val trends = calculator.calculateDailyTrend(
            listOf(booklet),
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31)
        )

        val day5 = trends.find { it.date == LocalDate.of(2025, 1, 5) }!!
        assertEquals(100.toAmount(), day5.income)
    }

    @Test
    fun `should scope to specific booklet only`() {
        val booklet1 = Booklet(1000.toAmount(), "Booklet 1")
        val booklet2 = Booklet(2000.toAmount(), "Booklet 2")

        booklet1.addTransaction(
            Transaction(UUID.randomUUID(), "B1 Income", LocalDate.of(2025, 1, 5), 300.toAmount(), isIncome = true)
        )
        booklet2.addTransaction(
            Transaction(UUID.randomUUID(), "B2 Income", LocalDate.of(2025, 1, 5), 700.toAmount(), isIncome = true)
        )

        val trendsAll = calculator.calculateDailyTrend(
            listOf(booklet1, booklet2),
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31)
        )

        val day5All = trendsAll.find { it.date == LocalDate.of(2025, 1, 5) }!!
        assertEquals(1000.toAmount(), day5All.income)
        assertEquals(2, day5All.totalBooklets)

        val trendsSingle = calculator.calculateDailyTrend(
            listOf(booklet1),
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31)
        )

        val day5Single = trendsSingle.find { it.date == LocalDate.of(2025, 1, 5) }!!
        assertEquals(300.toAmount(), day5Single.income)
        assertEquals(1, day5Single.totalBooklets)
    }

    @Test
    fun `should return all zero entries when no transactions in range`() {
        val booklet = Booklet(1000.toAmount(), "Booklet")

        val trends = calculator.calculateDailyTrend(
            listOf(booklet),
            LocalDate.of(2025, 3, 1),
            LocalDate.of(2025, 3, 31)
        )

        assertEquals(31, trends.size)
        trends.forEach { trend ->
            assertEquals(0.toAmount(), trend.income)
            assertEquals(0.toAmount(), trend.expenses)
            assertEquals(0.toAmount(), trend.balance)
        }
    }

    @Test
    fun `cumulative balance is seeded from a non-zero starting balance`() {
        val booklet = Booklet(0.toAmount(), "Booklet")
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income", LocalDate.of(2025, 1, 5), 500.toAmount(), isIncome = true)
        )

        val trends = calculator.calculateDailyTrend(
            booklets = listOf(booklet),
            startDate = LocalDate.of(2025, 1, 1),
            endDate = LocalDate.of(2025, 1, 31),
            startingBalance = 2000.toAmount()
        )

        val day1 = trends.find { it.date == LocalDate.of(2025, 1, 1) }!!
        assertEquals(BigDecimal("2000.00"), day1.cumulativeBalance.value)

        val day5 = trends.find { it.date == LocalDate.of(2025, 1, 5) }!!
        assertEquals(BigDecimal("2500.00"), day5.cumulativeBalance.value)
    }

    @Test
    fun `starting balance shifts all cumulative values uniformly`() {
        val booklet = Booklet(0.toAmount(), "Booklet")
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Expense", LocalDate.of(2025, 1, 10), 300.toAmount(), isIncome = false)
        )

        val trendsWithSeed = calculator.calculateDailyTrend(
            booklets = listOf(booklet),
            startDate = LocalDate.of(2025, 1, 1),
            endDate = LocalDate.of(2025, 1, 31),
            startingBalance = 1000.toAmount()
        )
        val trendsFromZero = calculator.calculateDailyTrend(
            booklets = listOf(booklet),
            startDate = LocalDate.of(2025, 1, 1),
            endDate = LocalDate.of(2025, 1, 31),
            startingBalance = 0.toAmount()
        )

        trendsWithSeed.zip(trendsFromZero).forEach { (seeded, zeroBased) ->
            assertEquals(
                BigDecimal("1000.00").add(zeroBased.cumulativeBalance.value),
                seeded.cumulativeBalance.value
            )
        }
    }

    @Test
    fun `should exclude transactions outside the date range`() {
        val booklet = Booklet(1000.toAmount(), "Booklet")
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Before", LocalDate.of(2025, 5, 24), 500.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Inside", LocalDate.of(2025, 5, 25), 100.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "After", LocalDate.of(2025, 6, 25), 800.toAmount(), isIncome = true)
        )

        val trends = calculator.calculateDailyTrend(
            listOf(booklet),
            LocalDate.of(2025, 5, 25),
            LocalDate.of(2025, 6, 24)
        )

        val totalIncome = trends.sumOf { it.income.value.toLong() }
        assertEquals(100L, totalIncome)
    }

    @Test
    fun `should handle multiple transactions on the same day`() {
        val booklet = Booklet(1000.toAmount(), "Booklet")
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income 1", LocalDate.of(2025, 1, 5), 200.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Income 2", LocalDate.of(2025, 1, 5), 300.toAmount(), isIncome = true)
        )
        booklet.addTransaction(
            Transaction(UUID.randomUUID(), "Expense 1", LocalDate.of(2025, 1, 5), 100.toAmount(), isIncome = false)
        )

        val trends = calculator.calculateDailyTrend(
            listOf(booklet),
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31)
        )

        val day5 = trends.find { it.date == LocalDate.of(2025, 1, 5) }!!
        assertEquals(500.toAmount(), day5.income)
        assertEquals(100.toAmount(), day5.expenses)
        assertEquals(400.toAmount(), day5.balance)
    }
}
