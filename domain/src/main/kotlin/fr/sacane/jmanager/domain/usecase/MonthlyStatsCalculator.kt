package fr.sacane.jmanager.domain.usecase


import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.MonthlyData
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.math.BigDecimal
import java.time.Month


interface MonthlyStatsCalculator {
    /**
     * Calculates and returns the monthly statistical data for a given list of transactions in a specific year.
     *
     * @param transactions the list of transactions to be analyzed
     * @param year the year for which the monthly statistics should be calculated
     * @return a list of MonthlyData representing income, expenses, and balance for each month of the specified year
     */
    fun calculateMonthlyStats(transactions: List<Transaction>, year: Int): List<MonthlyData>
}

@UseCase
class MonthlyStatsCalculatorImpl : MonthlyStatsCalculator {
    override fun calculateMonthlyStats(transactions: List<Transaction>, year: Int): List<MonthlyData> {
        val filteredTransactions = transactions.filter {
            it.date.year == year && !it.isPreview
        }

        return Month.entries.map { month ->
            val monthTransactions = filteredTransactions.filter {
                it.date.month == month
            }

            calculateMonthData(month.value, monthTransactions)
        }
    }

    private fun calculateMonthData(month: Int, transactions: List<Transaction>): MonthlyData {
        val income = transactions
            .filter { it.isIncome }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value) }

        val expenses = transactions
            .filter { !it.isIncome }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value) }

        val balance = income.subtract(expenses)

        return MonthlyData(
            month = month,
            income = Amount(income),
            expenses = Amount(expenses.abs()),
            balance = Amount(balance)
        )
    }
}