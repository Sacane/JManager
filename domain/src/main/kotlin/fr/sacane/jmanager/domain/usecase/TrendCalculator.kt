package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.MonthlyTrend
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth


interface TrendCalculator {

    /**
     * Calculates the monthly financial trends based on the provided list of booklets.
     *
     * @param booklets the list of booklets for which the financial trends will be calculated
     * @return a list of MonthlyTrend containing detailed financial data such as income, expenses, balance,
     *         cumulative balance, and the total number of booklets for each month
     */
    fun calculateTrend(
        booklets: List<Booklet>,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): List<MonthlyTrend>
}

@UseCase
class TrendCalculatorImpl : TrendCalculator {
    override fun calculateTrend(
        booklets: List<Booklet>,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<MonthlyTrend> {
        val monthsToCompute = resolveMonths(startDate, endDate)

        var cumulativeBalance = BigDecimal.ZERO

        return monthsToCompute.map { yearMonth ->
            val monthTransactions = booklets.flatMap { booklet ->
                booklet.transactions.filter { transaction ->
                    !transaction.isPreview &&
                            transaction.date.year == yearMonth.year &&
                            transaction.date.month == yearMonth.month &&
                            (startDate == null || !transaction.date.isBefore(startDate)) &&
                            (endDate == null || !transaction.date.isAfter(endDate))
                }
            }

            val income = monthTransactions
                .filter { it.isIncome }
                .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value) }

            val expenses = monthTransactions
                .filter { !it.isIncome }
                .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value.abs()) }

            val balance = income.subtract(expenses)
            cumulativeBalance = cumulativeBalance.add(balance)

            MonthlyTrend(
                month = yearMonth.monthValue,
                year = yearMonth.year,
                income = Amount(income),
                expenses = Amount(expenses),
                balance = Amount(balance),
                cumulativeBalance = Amount(cumulativeBalance),
                totalBooklets = booklets.size
            )
        }
    }

    private fun generateLast12Months(currentDate: LocalDate): List<YearMonth> {
        return (11 downTo 0).map { monthsAgo ->
            YearMonth.from(currentDate.minusMonths(monthsAgo.toLong()))
        }
    }

    private fun resolveMonths(startDate: LocalDate?, endDate: LocalDate?): List<YearMonth> {
        if (startDate == null || endDate == null) {
            return generateLast12Months(LocalDate.now())
        }

        val startMonth = YearMonth.from(startDate)
        val endMonth = YearMonth.from(endDate)
        val months = mutableListOf<YearMonth>()
        var current = startMonth
        while (!current.isAfter(endMonth)) {
            months.add(current)
            current = current.plusMonths(1)
        }
        return months
    }
}