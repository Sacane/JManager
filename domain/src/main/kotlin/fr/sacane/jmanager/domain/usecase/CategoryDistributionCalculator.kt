package fr.sacane.jmanager.domain.usecase


import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.CategoryData
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import java.awt.Color
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID



interface CategoryDistributionCalculator {
    /**
     * Calculates the distribution of transactions by category and the total amount.
     *
     * @param transactions the list of transactions to analyze
     * @return a pair where the first element is a list of `CategoryData` containing distribution data
     *         for each category, and the second element is the total amount for all transactions
     */
    fun calculateDistribution(
        transactions: List<Transaction>,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Pair<List<CategoryData>, Amount>
}

@UseCase
class CategoryDistributionCalculatorImpl(
    private val tagRepository: TagRepository
) : CategoryDistributionCalculator {

    private data class TagKey(val label: String, val id: UUID, val color: Color)

    override fun calculateDistribution(
        transactions: List<Transaction>,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Pair<List<CategoryData>, Amount> {
        val expenseTransactions = transactions.filter {
            !it.isIncome &&
                !it.isPreview &&
                (startDate == null || !it.date.isBefore(startDate)) &&
                (endDate == null || !it.date.isAfter(endDate))
        }

        val totalExpenses = expenseTransactions
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value.abs()) }

        if (totalExpenses == BigDecimal.ZERO) {
            return Pair(emptyList(), Amount(BigDecimal.ZERO))
        }

        val groupedByEffectiveParent = expenseTransactions.groupBy { transaction ->
            val tag = transaction.tag ?: tagRepository.defaultTag()
            val parentId = (tag as? Tag.Personal)?.parentId
            if (parentId != null) {
                val parent = tagRepository.findById(parentId)
                if (parent != null) TagKey(parent.label, parent.id!!, parent.color)
                else TagKey(tag.label, tag.id!!, tag.color)
            } else {
                TagKey(tag.label, tag.id!!, tag.color)
            }
        }

        val categoryData = groupedByEffectiveParent.flatMap { (parentKey, txs) ->
            val hasDirectTransactions = txs.any { tx ->
                val tag = tx.tag
                !(tag is Tag.Personal && tag.parentId != null)
            }

            if (hasDirectTransactions) {
                listOf(buildParentEntry(parentKey, txs, totalExpenses))
            } else {
                buildPromotedSubTagEntries(txs, totalExpenses)
            }
        }.sortedByDescending { it.totalAmount.value }

        return Pair(categoryData, Amount(totalExpenses))
    }

    private fun buildParentEntry(
        parentKey: TagKey,
        txs: List<Transaction>,
        totalExpenses: BigDecimal
    ): CategoryData {
        val parentTotal = txs.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value.abs()) }
        val percentage = parentTotal
            .divide(totalExpenses, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal("100"))

        val subGrouped = txs
            .filter { tx -> tx.tag is Tag.Personal && (tx.tag as Tag.Personal).parentId != null }
            .groupBy { tx ->
                val tag = tx.tag as Tag.Personal
                TagKey(tag.label, tag.id!!, tag.color)
            }

        val subCategories = subGrouped.map { (subKey, subTxs) ->
            val subTotal = subTxs.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value.abs()) }
            val subPercentage = subTotal
                .divide(parentTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
            CategoryData(
                tagLabel = subKey.label,
                tagId = subKey.id,
                tagColor = subKey.color,
                totalAmount = Amount(subTotal),
                percentage = subPercentage,
                transactionCount = subTxs.size
            )
        }.sortedByDescending { it.totalAmount.value }

        return CategoryData(
            tagLabel = parentKey.label,
            tagId = parentKey.id,
            tagColor = parentKey.color,
            totalAmount = Amount(parentTotal),
            percentage = percentage,
            transactionCount = txs.size,
            subCategories = subCategories
        )
    }

    private fun buildPromotedSubTagEntries(
        txs: List<Transaction>,
        totalExpenses: BigDecimal
    ): List<CategoryData> {
        return txs
            .groupBy { tx ->
                val tag = tx.tag as Tag.Personal
                TagKey(tag.label, tag.id!!, tag.color)
            }
            .map { (subKey, subTxs) ->
                val subTotal = subTxs.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount.value.abs()) }
                val subPercentage = subTotal
                    .divide(totalExpenses, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal("100"))
                CategoryData(
                    tagLabel = subKey.label,
                    tagId = subKey.id,
                    tagColor = subKey.color,
                    totalAmount = Amount(subTotal),
                    percentage = subPercentage,
                    transactionCount = subTxs.size
                )
            }
    }
}