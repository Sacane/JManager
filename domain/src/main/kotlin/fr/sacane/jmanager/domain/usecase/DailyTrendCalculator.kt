package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.DailyTrend
import java.math.BigDecimal
import java.time.LocalDate


interface DailyTrendCalculator {

    /**
     * Calculates daily financial trends for the provided booklets within the specified date range.
     *
     * @param booklets the list of booklets from which transactions are aggregated
     * @param startDate the first day of the range (inclusive)
     * @param endDate the last day of the range (inclusive)
     * @param startingBalance the actual account balance at the start of the period; seeds the cumulative accumulator
     * @return a list of DailyTrend, one entry per day in the range, ordered chronologically
     */
    fun calculateDailyTrend(
        booklets: List<Booklet>,
        startDate: LocalDate,
        endDate: LocalDate,
        startingBalance: Amount = Amount(BigDecimal.ZERO)
    ): List<DailyTrend>
}

@UseCase
class DailyTrendCalculatorImpl : DailyTrendCalculator {
    override fun calculateDailyTrend(
        booklets: List<Booklet>,
        startDate: LocalDate,
        endDate: LocalDate,
        startingBalance: Amount
    ): List<DailyTrend> {
        val days = generateDayRange(startDate, endDate)
        var cumulativeBalance = startingBalance.value

        return days.map { day ->
            val dayTransactions = booklets.flatMap { booklet ->
                booklet.transactions.filter { transaction ->
                    !transaction.isPreview && transaction.date == day
                }
            }

            val income = dayTransactions
                .filter { it.isIncome }
                .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value) }

            val expenses = dayTransactions
                .filter { !it.isIncome }
                .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value.abs()) }

            val balance = income.subtract(expenses)
            cumulativeBalance = cumulativeBalance.add(balance)

            DailyTrend(
                date = day,
                income = Amount(income),
                expenses = Amount(expenses),
                balance = Amount(balance),
                cumulativeBalance = Amount(cumulativeBalance),
                totalBooklets = booklets.size
            )
        }
    }

    private fun generateDayRange(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val days = mutableListOf<LocalDate>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            days.add(current)
            current = current.plusDays(1)
        }
        return days
    }
}
