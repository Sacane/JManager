package fr.sacane.jmanager.infrastructure.api.stats

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.math.BigDecimal
import java.time.LocalDate

data class MonthlyAccountStatsDTO(
    val accountId: Long,
    val accountLabel: String,
    val year: Int,
    val monthlyData: List<MonthlyDataDTO>
)

data class MonthlyDataDTO(
    val month: Int,
    val income: String,
    val expenses: String,
    val balance: String
)

data class CategoryDistributionDTO(
    val categories: List<CategoryDataDTO>,
    val totalExpenses: String
)

data class CategoryDataDTO(
    val tagLabel: String,
    val tagId: Long?,
    val totalAmount: String,
    val percentage: BigDecimal,
    val transactionCount: Int
)

data class TrendStatsDTO(
    val monthlyTrends: List<MonthlyTrendDTO>
)

data class MonthlyTrendDTO(
    val month: Int,
    val year: Int,
    val income: String,
    val expenses: String,
    val balance: String,
    val cumulativeBalance: String,
    val totalAccounts: Int
)

data class PrevisionalTransactionsDTO(
    val transactions: List<TransactionDTO>,
    val groupedByAccount: Map<String, List<TransactionDTO>>,
    val totalAmount: String,
    val totalIncome: String,
    val totalExpenses: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class TransactionDTO(
    val id: Long?,
    val label: String,
    val amount: String,
    val isIncome: Boolean,
    val date: LocalDate,
    val tag: String
)

// Extension functions pour convertir du domaine vers DTO
fun MonthlyAccountStatsOutput.toDTO() = MonthlyAccountStatsDTO(
    accountId = accountId,
    accountLabel = accountLabel,
    year = year,
    monthlyData = monthlyData.map { it.toDTO() }
)

fun MonthlyData.toDTO() = MonthlyDataDTO(
    month = month,
    income = income.toStringValue(),
    expenses = expenses.toStringValue(),
    balance = balance.toStringValue()
)

fun CategoryDistributionOutput.toDTO() = CategoryDistributionDTO(
    categories = categories.map { it.toDTO() },
    totalExpenses = totalExpenses.toStringValue()
)

fun CategoryData.toDTO() = CategoryDataDTO(
    tagLabel = tagLabel,
    tagId = tagId,
    totalAmount = totalAmount.toStringValue(),
    percentage = percentage,
    transactionCount = transactionCount
)

fun TrendStatsOutput.toDTO() = TrendStatsDTO(
    monthlyTrends = monthlyTrends.map { it.toDTO() }
)

fun MonthlyTrend.toDTO() = MonthlyTrendDTO(
    month = month,
    year = year,
    income = income.toStringValue(),
    expenses = expenses.toStringValue(),
    balance = balance.toStringValue(),
    cumulativeBalance = cumulativeBalance.toStringValue(),
    totalAccounts = totalAccounts
)

fun PrevisionalTransactionsOutput.toDTO() = PrevisionalTransactionsDTO(
    transactions = transactions.map { it.toTransactionDTO() },
    groupedByAccount = groupedByAccount.mapValues { (_, transactions) ->
        transactions.map { it.toTransactionDTO() }
    },
    totalAmount = totalAmount.toStringValue(),
    totalIncome = totalIncome.toStringValue(),
    totalExpenses = totalExpenses.toStringValue(),
    startDate = startDate,
    endDate = endDate
)

fun Transaction.toTransactionDTO() = TransactionDTO(
    id = id,
    label = label,
    amount = amount.toStringValue(),
    isIncome = isIncome,
    date = date,
    tag = tag.label
)