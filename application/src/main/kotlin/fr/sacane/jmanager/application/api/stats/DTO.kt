package fr.sacane.jmanager.application.api.stats

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.application.api.tag.ColorDTO
import fr.sacane.jmanager.application.api.toDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class MonthlyBookletStatsDTO(
    val bookletId: UUID,
    val bookletLabel: String,
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
    val tagId: UUID?,
    val colorDTO: ColorDTO,
    val totalAmount: String,
    val percentage: BigDecimal,
    val transactionCount: Int,
    val subCategories: List<CategoryDataDTO> = emptyList()
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
    val totalBooklets: Int
)

data class PrevisionalTransactionsDTO(
    val transactions: List<TransactionDTO>,
    val groupedByBooklet: Map<String, List<TransactionDTO>>,
    val totalAmount: String,
    val totalIncome: String,
    val totalExpenses: String,
    val regularTransactions: List<TransactionDTO>,
    val nonRegularTransactions: List<TransactionDTO>,
    val totalRegularAmount: String,
    val totalNonRegularAmount: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class TransactionDTO(
    val id: UUID?,
    val label: String,
    val amount: String,
    val isIncome: Boolean,
    val date: LocalDate,
    val tag: String,
    val regularTransactionId: String?
)

fun MonthlyBookletStatsOutput.toDTO() = MonthlyBookletStatsDTO(
    bookletId = bookletId,
    bookletLabel = bookletLabel,
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

fun CategoryData.toDTO(): CategoryDataDTO = CategoryDataDTO(
    tagLabel = tagLabel,
    tagId = tagId,
    colorDTO = tagColor.toDTO(),
    totalAmount = totalAmount.toStringValue(),
    percentage = percentage,
    transactionCount = transactionCount,
    subCategories = subCategories.map { it.toDTO() }
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
    totalBooklets = totalBooklets
)

data class DailyTrendStatsDTO(
    val dailyTrends: List<DailyTrendDTO>
)

data class DailyTrendDTO(
    val date: LocalDate,
    val income: String,
    val expenses: String,
    val balance: String,
    val cumulativeBalance: String,
    val totalBooklets: Int
)

fun DailyTrendStatsOutput.toDTO() = DailyTrendStatsDTO(
    dailyTrends = dailyTrends.map { it.toDTO() }
)

fun DailyTrend.toDTO() = DailyTrendDTO(
    date = date,
    income = income.toStringValue(),
    expenses = expenses.toStringValue(),
    balance = balance.toStringValue(),
    cumulativeBalance = cumulativeBalance.toStringValue(),
    totalBooklets = totalBooklets
)

fun PrevisionalTransactionsOutput.toDTO(defaultTag: Tag) = PrevisionalTransactionsDTO(
    transactions = transactions.map { it.toTransactionDTO(defaultTag) },
    groupedByBooklet = groupedByBooklet.mapValues { (_, transactions) ->
        transactions.map { it.toTransactionDTO(defaultTag) }
    },
    totalAmount = totalAmount.toStringValue(),
    totalIncome = totalIncome.toStringValue(),
    totalExpenses = totalExpenses.toStringValue(),
    regularTransactions = regularTransactions.map { it.toTransactionDTO(defaultTag) },
    nonRegularTransactions = nonRegularTransactions.map { it.toTransactionDTO(defaultTag) },
    totalRegularAmount = totalRegularAmount.toStringValue(),
    totalNonRegularAmount = totalNonRegularAmount.toStringValue(),
    startDate = startDate,
    endDate = endDate
)

fun Transaction.toTransactionDTO(defaultTag: Tag) = TransactionDTO(
    id = id,
    label = label,
    amount = amount.toStringValue(),
    isIncome = isIncome,
    date = date,
    tag = tag?.label ?: defaultTag.label,
    regularTransactionId = regularTransactionId?.value
)