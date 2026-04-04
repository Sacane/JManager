package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.math.BigDecimal
import java.util.UUID


data class MonthlyAccountStatsOutput(
    val bookletId: UUID,
    val bookletLabel: String,
    val year: Int,
    val monthlyData: List<MonthlyData>
)

data class MonthlyData(
    val month: Int,
    val income: Amount,
    val expenses: Amount,
    val balance: Amount
)


data class CategoryDistributionOutput(
    val categories: List<CategoryData>,
    val totalExpenses: Amount
)

data class CategoryData(
    val tagLabel: String,
    val tagId: UUID?,
    val totalAmount: Amount,
    val percentage: BigDecimal,
    val transactionCount: Int
)


data class TrendStatsOutput(
    val monthlyTrends: List<MonthlyTrend>
)

data class MonthlyTrend(
    val month: Int,
    val year: Int,
    val income: Amount,
    val expenses: Amount,
    val balance: Amount,
    val cumulativeBalance: Amount,
    val totalAccounts: Int
)

data class PrevisionalTransactionsOutput(
    val transactions: List<Transaction>,
    val groupedByAccount: Map<String, List<Transaction>>,
    val totalAmount: Amount,
    val totalIncome: Amount,
    val totalExpenses: Amount,
    val regularTransactions: List<Transaction> = transactions.filter { it.regularTransactionId != null },
    val nonRegularTransactions: List<Transaction> = transactions.filter { it.regularTransactionId == null },
    val totalRegularAmount: Amount = Amount(
        regularTransactions.fold(BigDecimal.ZERO) { acc, transaction ->
            val signedAmount = if (transaction.isIncome) transaction.amount.value else transaction.amount.value.abs().negate()
            acc.add(signedAmount)
        }
    ),
    val totalNonRegularAmount: Amount = Amount(
        nonRegularTransactions.fold(BigDecimal.ZERO) { acc, transaction ->
            val signedAmount = if (transaction.isIncome) transaction.amount.value else transaction.amount.value.abs().negate()
            acc.add(signedAmount)
        }
    ),
    val startDate: java.time.LocalDate,
    val endDate: java.time.LocalDate
)