package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.math.BigDecimal
import java.util.UUID


data class MonthlyAccountStatsOutput(
    val accountId: UUID,
    val accountLabel: String,
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
    val startDate: java.time.LocalDate,
    val endDate: java.time.LocalDate
)