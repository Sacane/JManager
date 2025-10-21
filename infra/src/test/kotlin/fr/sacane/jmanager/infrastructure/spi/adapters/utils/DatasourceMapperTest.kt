package fr.sacane.jmanager.infrastructure.spi.adapters.utils

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.infrastructure.spi.entity.BookletResource
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import fr.sacane.jmanager.infrastructure.spi.entity.TransactionResource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.Color
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class DatasourceMapperTest {

    @Test
    fun `Transaction should convert to TransactionResource with default tag`() {
        val tag = Tag(
            label = "Shopping",
            id = 1L,
            color = Color.RED,
            isDefault = true
        )
        val transaction = Transaction(
            id = 1L,
            label = "Buy shoes",
            date = LocalDate.of(2024, 6, 15),
            amount = 100.toAmount(),
            isIncome = false,
            tag = tag
        )

        val defaultTagResource = DefaultTagResource(
            name = "Shopping",
            color = fr.sacane.jmanager.infrastructure.spi.entity.Color(
                red = 255,
                green = 0,
                blue = 0
            )
        )
        val resource = transaction.asResource(defaultTagResource)

        assertEquals("Buy shoes", resource.label)
        assertEquals(LocalDate.of(2024, 6, 15), resource.date)
        assertEquals(BigDecimal("100.00"), resource.value)
        assertFalse(resource.isIncome!!)
        assertEquals(1L, resource.idSheet)
        assertNotNull(resource.tag)
        assertEquals("Shopping", resource.tag?.name)
    }

    @Test
    fun `Transaction should convert to TransactionResource with personal tag`() {
        val tag = Tag(
            label = "My Category",
            id = 2L,
            color = Color.BLUE,
            isDefault = false
        )
        val transaction = Transaction(
            id = 2L,
            label = "Custom expense",
            date = LocalDate.of(2024, 6, 20),
            amount = 50.toAmount(),
            isIncome = false,
            tag = tag
        )

        val personalTag = TagPersonalResource(
            name = "My Category",
            color = fr.sacane.jmanager.infrastructure.spi.entity.Color(
                red = 0,
                green = 0,
                blue = 255
            )
        )
        val resource = transaction.asResource(personalTag)

        assertEquals("Custom expense", resource.label)
        assertEquals(50.toAmount().value, resource.value)
        assertNotNull(resource.personalTag)
        assertEquals("My Category", resource.personalTag?.name)
    }

    @Test
    fun `Transaction should convert to TransactionResource without tag`() {
        val transaction = Transaction(
            id = 3L,
            label = "No tag transaction",
            date = LocalDate.of(2024, 7, 1),
            amount = 200.toAmount(),
            isIncome = true,
            tag = null
        )

        val resource = transaction.asResource(null)

        assertEquals("No tag transaction", resource.label)
        assertEquals(200.toAmount().value, resource.value)
        assertTrue(resource.isIncome!!)
        assertNull(resource.tag)
        assertNull(resource.personalTag)
    }

    @Test
    fun `Transaction should preserve preview status when converting to resource`() {
        val transaction = Transaction(
            id = 4L,
            label = "Preview transaction",
            date = LocalDate.now().plusDays(5),
            amount = 150.toAmount(),
            isIncome = true,
            isPreview = true,
            tag = null
        )

        val resource = transaction.asResource(null)

        assertTrue(resource.isPreview)
    }

    @Test
    fun `TransactionResource should convert to Transaction model`() {
        val resource = TransactionResource(label = "Salary")
        resource.idSheet = 10L
        resource.date = LocalDate.of(2024, 8, 1)
        resource.value = BigDecimal("3000.00")
        resource.isIncome = true
        resource.lastModified = LocalDateTime.now()
        resource.isPreview = false
        resource.tag = DefaultTagResource(
            name = "Income",
            color = fr.sacane.jmanager.infrastructure.spi.entity.Color(
                red = 0,
                green = 255,
                blue = 0
            )
        )

        val transaction = resource.toModel()

        assertEquals(10L, transaction.id)
        assertEquals("Salary", transaction.label)
        assertEquals(LocalDate.of(2024, 8, 1), transaction.date)
        assertEquals(3000.toAmount(), transaction.amount)
        assertTrue(transaction.isIncome)
        assertFalse(transaction.isPreview)
        assertNotNull(transaction.tag)
    }

    @Test
    fun `Booklet should convert to BookletResource`() {
        val transaction = Transaction(
            id = 1L,
            label = "Test transaction",
            date = LocalDate.now(),
            amount = 50.toAmount(),
            isIncome = true,
            tag = null
        )
        val booklet = Booklet(
            amount = 1000.toAmount(),
            labelAccount = "Main Account",
            id = 5L
        )
        booklet.addTransaction(transaction)

        val resource = booklet.asResource()

        assertEquals(5L, resource.idAccount)
        assertEquals("Main Account", resource.label)
        assertEquals(1, resource.sheets.size)
        assertEquals("Test transaction", resource.sheets[0].label)
    }

    @Test
    fun `Booklet should convert to BookletResource with empty transactions`() {
        val booklet = Booklet(
            amount = 500.toAmount(),
            labelAccount = "Empty Account",
            id = 6L
        )

        val resource = booklet.asResource()

        assertEquals(6L, resource.idAccount)
        assertEquals("Empty Account", resource.label)
        assertTrue(resource.sheets.isEmpty())
    }

    @Test
    fun `BookletResource should convert to Booklet model`() {
        val transactionResource = TransactionResource(label = "Test")
        transactionResource.idSheet = 1L
        transactionResource.date = LocalDate.now()
        transactionResource.value = BigDecimal("100.00")
        transactionResource.isIncome = true

        val bookletResource = BookletResource(
            idAccount = 7L,
            amount = BigDecimal("2000.00"),
            label = "Test Booklet",
            sheets = mutableListOf(transactionResource),
            initialSold = BigDecimal("2000.00"),
            previewAmount = BigDecimal("2000.00")
        )

        val booklet = bookletResource.toModel()

        assertEquals(7L, booklet.id)
        assertEquals("Test Booklet", booklet.label)
        assertEquals(2000.toAmount(), booklet.amount)
        assertEquals(1, booklet.transactions.size)
        assertEquals("Test", booklet.transactions[0].label)
    }

    @Test
    fun `Transaction should preserve lastModified when converting`() {
        val now = LocalDateTime.now()
        val transaction = Transaction(
            id = 8L,
            label = "Modified transaction",
            date = LocalDate.now(),
            amount = 75.toAmount(),
            isIncome = false,
            lastModified = now,
            tag = null
        )

        val resource = transaction.asResource(null)

        assertEquals(now, resource.lastModified)
    }

    @Test
    fun `Booklet should preserve initial sold and preview amount when converting`() {
        val booklet = Booklet(
            amount = 1000.toAmount(),
            labelAccount = "Test",
            initialSold = 800.toAmount(),
            previewAmount = 950.toAmount(),
            id = 9L
        )

        val resource = booklet.asResource()

        assertEquals(BigDecimal("800.00"), resource.initialSold)
        assertEquals(BigDecimal("950.00"), resource.previewAmount)
    }

    @Test
    fun `TransactionResource with personal tag should convert to Transaction model`() {
        val resource = TransactionResource(label = "Personal expense")
        resource.idSheet = 11L
        resource.date = LocalDate.of(2024, 9, 1)
        resource.value = BigDecimal("250.00")
        resource.isIncome = false
        resource.personalTag = TagPersonalResource(
            name = "My Tag",
            color = fr.sacane.jmanager.infrastructure.spi.entity.Color(
                red = 255,
                green = 255,
                blue = 0
            )
        )

        val transaction = resource.toModel()

        assertEquals("Personal expense", transaction.label)
        assertEquals(250.toAmount(), transaction.amount)
        assertNotNull(transaction.tag)
        assertEquals("My Tag", transaction.tag?.label)
    }

    @Test
    fun `TransactionResource without tag should use default Aucune tag`() {
        val resource = TransactionResource(label = "No tag")
        resource.idSheet = 12L
        resource.date = LocalDate.now()
        resource.value = BigDecimal("100.00")
        resource.isIncome = true

        val transaction = resource.toModel()

        assertNotNull(transaction.tag)
        assertEquals("Aucune", transaction.tag?.label)
        assertEquals(Color(0, 0, 0), transaction.tag?.color)
    }
}
