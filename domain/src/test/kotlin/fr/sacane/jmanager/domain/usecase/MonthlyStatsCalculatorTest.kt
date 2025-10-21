package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class MonthlyStatsCalculatorTest {

    private lateinit var calculator: MonthlyStatsCalculator

    @BeforeEach
    fun setup() {
        calculator = MonthlyStatsCalculatorImpl()
    }

    @Test
    fun `calculateMonthlyStats should return 12 months of data`() {
        val transactions = emptyList<Transaction>()

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        assertEquals(12, monthlyStats.size)
    }

    @Test
    fun `calculateMonthlyStats should filter transactions by year`() {
        val transactions = listOf(
            Transaction(1L, "2024", LocalDate.of(2024, 3, 15), 100.toAmount(), true),
            Transaction(2L, "2023", LocalDate.of(2023, 3, 15), 200.toAmount(), true),
            Transaction(3L, "2025", LocalDate.of(2025, 3, 15), 300.toAmount(), true)
        )

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        val marchStats = monthlyStats[2] // March is index 2 (0-based)
        assertEquals(100.toAmount(), marchStats.income)
    }

    @Test
    fun `calculateMonthlyStats should ignore preview transactions`() {
        val transactions = listOf(
            Transaction(1L, "Regular", LocalDate.of(2024, 5, 15), 100.toAmount(), true, isPreview = false),
            Transaction(2L, "Preview", LocalDate.of(2024, 5, 20), 200.toAmount(), true, isPreview = true)
        )

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        val mayStats = monthlyStats[4] // May is index 4
        assertEquals(100.toAmount(), mayStats.income)
    }

    @Test
    fun `calculateMonthlyStats should separate income and expenses`() {
        val transactions = listOf(
            Transaction(1L, "Income", LocalDate.of(2024, 6, 15), 500.toAmount(), true),
            Transaction(2L, "Expense", LocalDate.of(2024, 6, 20), (-200).toAmount(), false)
        )

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        val juneStats = monthlyStats[5] // June is index 5
        assertEquals(500.toAmount(), juneStats.income)
        assertEquals(200.toAmount(), juneStats.expenses)
        assertEquals(300.toAmount(), juneStats.balance)
    }

    @Test
    fun `calculateMonthlyStats should calculate correct balance`() {
        val transactions = listOf(
            Transaction(1L, "Income 1", LocalDate.of(2024, 7, 10), 1000.toAmount(), true),
            Transaction(2L, "Income 2", LocalDate.of(2024, 7, 15), 500.toAmount(), true),
            Transaction(3L, "Expense 1", LocalDate.of(2024, 7, 20), (-300).toAmount(), false),
            Transaction(4L, "Expense 2", LocalDate.of(2024, 7, 25), (-200).toAmount(), false)
        )

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        val julyStats = monthlyStats[6] // July is index 6
        assertEquals(1500.toAmount(), julyStats.income)
        assertEquals(500.toAmount(), julyStats.expenses)
        assertEquals(1000.toAmount(), julyStats.balance)
    }

    @Test
    fun `calculateMonthlyStats should return zero amounts for months without transactions`() {
        val transactions = listOf(
            Transaction(1L, "Income", LocalDate.of(2024, 1, 15), 100.toAmount(), true)
        )

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        val februaryStats = monthlyStats[1] // February
        assertEquals(0.toAmount(), februaryStats.income)
        assertEquals(0.toAmount(), februaryStats.expenses)
        assertEquals(0.toAmount(), februaryStats.balance)
    }

    @Test
    fun `calculateMonthlyStats should group transactions by month correctly`() {
        val transactions = listOf(
            Transaction(1L, "Jan", LocalDate.of(2024, 1, 15), 100.toAmount(), true),
            Transaction(2L, "Jan", LocalDate.of(2024, 1, 20), 50.toAmount(), true),
            Transaction(3L, "Feb", LocalDate.of(2024, 2, 15), 200.toAmount(), true)
        )

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        assertEquals(150.toAmount(), monthlyStats[0].income) // January
        assertEquals(200.toAmount(), monthlyStats[1].income) // February
    }

    @Test
    fun `calculateMonthlyStats should handle expenses as absolute values`() {
        val transactions = listOf(
            Transaction(1L, "Expense", LocalDate.of(2024, 3, 15), (-250).toAmount(), false)
        )

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        val marchStats = monthlyStats[2]
        assertEquals(250.toAmount(), marchStats.expenses)
    }

    @Test
    fun `calculateMonthlyStats should set correct month numbers`() {
        val transactions = emptyList<Transaction>()

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        assertEquals(1, monthlyStats[0].month)
        assertEquals(12, monthlyStats[11].month)
    }

    @Test
    fun `calculateMonthlyStats should handle only income transactions`() {
        val transactions = listOf(
            Transaction(1L, "Income 1", LocalDate.of(2024, 4, 10), 300.toAmount(), true),
            Transaction(2L, "Income 2", LocalDate.of(2024, 4, 20), 700.toAmount(), true)
        )

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        val aprilStats = monthlyStats[3]
        assertEquals(1000.toAmount(), aprilStats.income)
        assertEquals(0.toAmount(), aprilStats.expenses)
        assertEquals(1000.toAmount(), aprilStats.balance)
    }

    @Test
    fun `calculateMonthlyStats should handle only expense transactions`() {
        val transactions = listOf(
            Transaction(1L, "Expense 1", LocalDate.of(2024, 8, 10), (-400).toAmount(), false),
            Transaction(2L, "Expense 2", LocalDate.of(2024, 8, 20), (-600).toAmount(), false)
        )

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        val augustStats = monthlyStats[7]
        assertEquals(0.toAmount(), augustStats.income)
        assertEquals(1000.toAmount(), augustStats.expenses)
        assertEquals((-1000).toAmount(), augustStats.balance)
    }

    @Test
    fun `calculateMonthlyStats should handle December transactions`() {
        val transactions = listOf(
            Transaction(1L, "December income", LocalDate.of(2024, 12, 25), 5000.toAmount(), true),
            Transaction(2L, "December expense", LocalDate.of(2024, 12, 31), (-1000).toAmount(), false)
        )

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        val decemberStats = monthlyStats[11]
        assertEquals(5000.toAmount(), decemberStats.income)
        assertEquals(1000.toAmount(), decemberStats.expenses)
        assertEquals(4000.toAmount(), decemberStats.balance)
    }

    @Test
    fun `calculateMonthlyStats should handle leap year February`() {
        val transactions = listOf(
            Transaction(1L, "Feb 29", LocalDate.of(2024, 2, 29), 100.toAmount(), true)
        )

        val monthlyStats = calculator.calculateMonthlyStats(transactions, 2024)

        val februaryStats = monthlyStats[1]
        assertEquals(100.toAmount(), februaryStats.income)
    }
}

