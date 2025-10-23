package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Color
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

val generatedTagId: UUID = UUID.randomUUID()

class CategoryDistributionCalculatorTest {

    private lateinit var calculator: CategoryDistributionCalculator
    private lateinit var tagRepository: FakeTagRepository

    @BeforeEach
    fun setup() {
        tagRepository = FakeTagRepository()
        calculator = CategoryDistributionCalculatorImpl(tagRepository)
    }

    @Test
    fun `calculateDistribution should return empty list when no transactions`() {
        val (categories, totalExpenses) = calculator.calculateDistribution(emptyList())

        assertTrue(categories.isEmpty())
        assertEquals(Amount(BigDecimal.ZERO), totalExpenses)
    }

    @Test
    fun `calculateDistribution should return empty list when no expense transactions`() {
        val transactions = listOf(
            Transaction(UUID.randomUUID(), "Income", LocalDate.now(), 1000.toAmount(), isIncome = true)
        )

        val (categories, totalExpenses) = calculator.calculateDistribution(transactions)

        assertTrue(categories.isEmpty())
        assertEquals(Amount(BigDecimal.ZERO), totalExpenses)
    }

    @Test
    fun `calculateDistribution should ignore preview transactions`() {
        val transactions = listOf(
            Transaction(UUID.randomUUID(), "Expense", LocalDate.now(), 100.toAmount(), isIncome = false, isPreview = true)
        )

        val (categories, totalExpenses) = calculator.calculateDistribution(transactions)

        assertTrue(categories.isEmpty())
        assertEquals(Amount(BigDecimal.ZERO), totalExpenses)
    }

    @Test
    fun `calculateDistribution should calculate correct distribution for single category`() {
        val tagId = UUID.randomUUID()
        val shoppingTag = Tag("Shopping", id = tagId, color = Color.RED)
        val transactions = listOf(
            Transaction(UUID.randomUUID(), "Buy shoes", LocalDate.now(), 50.toAmount(), isIncome = false, tag = shoppingTag),
            Transaction(UUID.randomUUID(), "Buy clothes", LocalDate.now(), 30.toAmount(), isIncome = false, tag = shoppingTag)
        )

        val (categories, totalExpenses) = calculator.calculateDistribution(transactions)

        assertEquals(1, categories.size)
        assertEquals("Shopping", categories[0].tagLabel)
        assertEquals(tagId, categories[0].tagId)
        assertEquals(80.toAmount(), categories[0].totalAmount)
        assertEquals(BigDecimal("100.0000"), categories[0].percentage)
        assertEquals(2, categories[0].transactionCount)
        assertEquals(80.toAmount(), totalExpenses)
    }

    @Test
    fun `calculateDistribution should calculate correct distribution for multiple categories`() {
        val shoppingTag = Tag("Shopping", id = UUID.randomUUID(), color = Color.RED)
        val foodTag = Tag("Food", id = UUID.randomUUID(), color = Color.GREEN)

        val transactions = listOf(
            Transaction(UUID.randomUUID(), "Shopping", LocalDate.now(), 100.toAmount(), isIncome = false, tag = shoppingTag),
            Transaction(UUID.randomUUID(), "Food", LocalDate.now(), 50.toAmount(), isIncome = false, tag = foodTag),
            Transaction(UUID.randomUUID(), "More shopping", LocalDate.now(), 50.toAmount(), isIncome = false, tag = shoppingTag)
        )

        val (categories, totalExpenses) = calculator.calculateDistribution(transactions)

        assertEquals(2, categories.size)
        assertEquals(200.toAmount(), totalExpenses)

        assertEquals("Shopping", categories[0].tagLabel)
        assertEquals(150.toAmount(), categories[0].totalAmount)
        assertEquals(BigDecimal("75.0000"), categories[0].percentage)
        assertEquals(2, categories[0].transactionCount)

        assertEquals("Food", categories[1].tagLabel)
        assertEquals(50.toAmount(), categories[1].totalAmount)
        assertEquals(BigDecimal("25.0000"), categories[1].percentage)
        assertEquals(1, categories[1].transactionCount)
    }

    @Test
    fun `calculateDistribution should use default tag when transaction has no tag`() {
        val id = UUID.randomUUID()
        val transactions = listOf(
            Transaction(id, "No tag expense", LocalDate.now(), 100.toAmount(), isIncome = false, tag = null)
        )

        val (categories, totalExpenses) = calculator.calculateDistribution(transactions)

        assertEquals(1, categories.size)
        assertEquals("Default", categories[0].tagLabel)
        assertEquals(generatedTagId, categories[0].tagId)
        assertEquals(100.toAmount(), categories[0].totalAmount)
        assertEquals(1, categories[0].transactionCount)
    }

    @Test
    fun `calculateDistribution should handle mixed income and expense transactions`() {
        val tag = Tag("Mixed", id = UUID.randomUUID())
        val transactions = listOf(
            Transaction(UUID.randomUUID(), "Income", LocalDate.now(), 500.toAmount(), isIncome = true, tag = tag),
            Transaction(UUID.randomUUID(), "Expense", LocalDate.now(), 100.toAmount(), isIncome = false, tag = tag)
        )

        val (categories, totalExpenses) = calculator.calculateDistribution(transactions)

        assertEquals(1, categories.size)
        assertEquals(100.toAmount(), totalExpenses)
        assertEquals(1, categories[0].transactionCount) // Only expenses count
    }

    @Test
    fun `calculateDistribution should sort categories by amount descending`() {
        val tag1 = Tag("Small", id = UUID.randomUUID())
        val tag2 = Tag("Large", id = UUID.randomUUID())
        val tag3 = Tag("Medium", id = UUID.randomUUID())

        val transactions = listOf(
            Transaction(UUID.randomUUID(), "Small", LocalDate.now(), 10.toAmount(), isIncome = false, tag = tag1),
            Transaction(UUID.randomUUID(), "Large", LocalDate.now(), 100.toAmount(), isIncome = false, tag = tag2),
            Transaction(UUID.randomUUID(), "Medium", LocalDate.now(), 50.toAmount(), isIncome = false, tag = tag3)
        )

        val (categories, _) = calculator.calculateDistribution(transactions)

        assertEquals("Large", categories[0].tagLabel)
        assertEquals("Medium", categories[1].tagLabel)
        assertEquals("Small", categories[2].tagLabel)
    }

    @Test
    fun `calculateDistribution should handle absolute values correctly`() {
        val tag = Tag("Test", id = UUID.randomUUID())
        val transactions = listOf(
            Transaction(UUID.randomUUID(), "Negative amount", LocalDate.now(), 100.toAmount(), isIncome = false, tag = tag)
        )

        val (categories, totalExpenses) = calculator.calculateDistribution(transactions)

        assertEquals(100.toAmount(), categories[0].totalAmount)
        assertEquals(100.toAmount(), totalExpenses)
    }

    private class FakeTagRepository : TagRepository {
        override fun save(userId: UserId, tag: Tag): Tag = tag
        override fun getAll(userId: UserId): List<Tag> = emptyList()
        override fun deleteByLabel(label: String) {}
        override fun getAllDefault(userId: UserId): List<Tag> = emptyList()
        override fun existsByLabelAndUserId(userId: UserId, tag: Tag): Boolean = false
        override fun saveAll(defaultTags: List<Tag>) {}
        override fun existsDefault(): Boolean = true
        override fun deleteById(tagId: UUID): Boolean = false
        override fun defaultTag(): Tag = Tag("Default", id = generatedTagId)
        override fun patch(tag: Tag): Tag = tag
        override fun existsAnotherTagByLabel(userId: UserId, tag: Tag): Boolean = false
        override fun existsById(tagId: UUID): Boolean = false
    }
}

