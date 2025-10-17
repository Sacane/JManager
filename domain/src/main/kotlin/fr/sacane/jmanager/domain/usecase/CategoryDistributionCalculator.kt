package fr.sacane.jmanager.domain.usecase


import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.CategoryData
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.TagRepository
import java.math.BigDecimal
import java.math.RoundingMode



interface CategoryDistributionCalculator {
    /**
     * Calculates the distribution of transactions by category and the total amount.
     *
     * @param transactions the list of transactions to analyze
     * @return a pair where the first element is a list of `CategoryData` containing distribution data
     *         for each category, and the second element is the total amount for all transactions
     */
    fun calculateDistribution(transactions: List<Transaction>): Pair<List<CategoryData>, Amount>
}

@UseCase
class CategoryDistributionCalculatorImpl(
    private val tagRepository: TagRepository
) : CategoryDistributionCalculator {

    override fun calculateDistribution(transactions: List<Transaction>): Pair<List<CategoryData>, Amount> {
        val expenseTransactions = transactions.filter {
            !it.isIncome && !it.isPreview
        }

        val totalExpenses = expenseTransactions
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value.abs()) }

        if (totalExpenses == BigDecimal.ZERO) {
            return Pair(emptyList(), Amount(BigDecimal.ZERO))
        }


        val groupedByTag = expenseTransactions.groupBy { transaction ->
            val tag = transaction.tag ?: tagRepository.defaultTag()
            tag.label to tag.id!!
        }

        val categoryData = groupedByTag.map { (tagInfo, transactions) ->
            val tagTotal = transactions
                .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value.abs()) }

            val percentage = tagTotal
                .divide(totalExpenses, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))

            CategoryData(
                tagLabel = tagInfo.first,
                tagId = tagInfo.second,
                totalAmount = Amount(tagTotal),
                percentage = percentage,
                transactionCount = transactions.size
            )
        }.sortedByDescending { it.totalAmount.value }

        return Pair(categoryData, Amount(totalExpenses))
    }
}