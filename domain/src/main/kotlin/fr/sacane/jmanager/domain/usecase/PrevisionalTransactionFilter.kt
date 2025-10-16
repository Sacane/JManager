package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.math.BigDecimal
import java.time.LocalDate

interface PrevisionalTransactionFilter {

    /**
     * Filters and retrieves previsional transactions from a list of booklets within a specified date range.
     *
     * @param booklets the list of booklets containing transactions to be filtered
     * @param startDate the starting date of the range for filtering transactions
     * @param endDate the ending date of the range for filtering transactions
     * @return a result containing the filtered previsional transactions, grouped by account,
     *         along with the total amount, total income, and total expenses within the specified range
     */
    fun filterPrevisionalTransactions(
        booklets: List<Booklet>,
        startDate: LocalDate,
        endDate: LocalDate
    ): PrevisionalTransactionResult
}

data class PrevisionalTransactionResult(
    val transactions: List<Transaction>,
    val groupedByAccount: Map<String, List<Transaction>>,
    val totalAmount: Amount,
    val totalIncome: Amount,
    val totalExpenses: Amount
)
@UseCase
class PrevisionalTransactionFilterImpl : PrevisionalTransactionFilter {
    override fun filterPrevisionalTransactions(
        booklets: List<Booklet>,
        startDate: LocalDate,
        endDate: LocalDate
    ): PrevisionalTransactionResult {
        val previsionalTransactions = booklets.flatMap { booklet ->
            booklet.transactions
                .filter { transaction ->
                    transaction.isPreview &&
                            !transaction.date.isBefore(startDate) &&
                            !transaction.date.isAfter(endDate)
                }
        }.sortedBy { it.date }

        val groupedByAccount = booklets.associate { booklet ->
            booklet.label to booklet.transactions.filter { transaction ->
                transaction.isPreview &&
                        !transaction.date.isBefore(startDate) &&
                        !transaction.date.isAfter(endDate)
            }.sortedBy { it.date }
        }.filterValues { it.isNotEmpty() }

        val totalIncome = previsionalTransactions
            .filter { it.amount.value > BigDecimal.ZERO }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value) }

        val totalExpenses = previsionalTransactions
            .filter { it.amount.value < BigDecimal.ZERO }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value.abs()) }

        val totalAmount = totalIncome.subtract(totalExpenses)

        return PrevisionalTransactionResult(
            transactions = previsionalTransactions,
            groupedByAccount = groupedByAccount,
            totalAmount = Amount(totalAmount),
            totalIncome = Amount(totalIncome),
            totalExpenses = Amount(totalExpenses)
        )
    }
}