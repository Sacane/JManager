package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.asPersonalTag
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.models.transaction.Transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class CsvTransactionExporterTest {

    private lateinit var exporter: CsvTransactionExporter

    @BeforeEach
    fun setup() {
        exporter = CsvTransactionExporter()
    }

    @Test
    fun `exportToCsv should return only header when list is empty`() {
        val transactions = emptyList<Transaction>()

        val csvContent = exporter.exportToCsv(transactions)

        assertEquals("date;label;depense;recette;tag", csvContent)
    }

    @Test
    fun `exportToCsv should export a single expense transaction correctly`() {
        val alimentationTag = defaultTags.find { it.label == "Alimentation & Restaurant" }
        
        val transaction = Transaction(
            id = UUID.randomUUID(),
            label = "Courses alimentaires",
            date = LocalDate.of(2025, 1, 15),
            amount = Amount(BigDecimal("45.50")),
            isIncome = false,
            tag = alimentationTag
        )

        val csvContent = exporter.exportToCsv(listOf(transaction))

        val lines = csvContent.split("\n")
        assertEquals(2, lines.size)
        assertEquals("date;label;depense;recette;tag", lines[0])
        assertEquals("15-01-2025;Courses alimentaires;45,50;;Alimentation & Restaurant", lines[1])
    }

    @Test
    fun `exportToCsv should export a single income transaction correctly`() {
        val transaction = Transaction(
            id = UUID.randomUUID(),
            label = "Salaire",
            date = LocalDate.of(2025, 1, 20),
            amount = Amount(BigDecimal("2500.00")),
            isIncome = true,
            tag = null
        )

        val csvContent = exporter.exportToCsv(listOf(transaction))

        val lines = csvContent.split("\n")
        assertEquals(2, lines.size)
        assertEquals("date;label;depense;recette;tag", lines[0])
        assertEquals("20-01-2025;Salaire;;2500,00;", lines[1])
    }

    @Test
    fun `exportToCsv should filter out preview transactions`() {
        val regularTransaction = Transaction(
            id = UUID.randomUUID(),
            label = "Transaction normale",
            date = LocalDate.of(2025, 1, 15),
            amount = Amount(BigDecimal("100.00")),
            isIncome = false,
            isPreview = false
        )

        val previewTransaction = Transaction(
            id = UUID.randomUUID(),
            label = "Transaction prévisionnelle",
            date = LocalDate.of(2025, 2, 15),
            amount = Amount(BigDecimal("200.00")),
            isIncome = false,
            isPreview = true
        )

        val csvContent = exporter.exportToCsv(listOf(regularTransaction, previewTransaction))

        val lines = csvContent.split("\n")
        assertEquals(2, lines.size) // Header + 1 transaction only
        assertEquals("date;label;depense;recette;tag", lines[0])
        assertTrue(lines[1].contains("Transaction normale"))
        assertFalse(lines[1].contains("Transaction prévisionnelle"))
    }

    @Test
    fun `exportToCsv should export multiple transactions correctly`() {
        val alimentationTag = "Alimentation".asPersonalTag()
        val transportTag = defaultTags.find { it.label == "Transport" }
        
        val transactions = listOf(
            Transaction(
                id = UUID.randomUUID(),
                label = "Courses",
                date = LocalDate.of(2025, 1, 10),
                amount = Amount(BigDecimal("45.50")),
                isIncome = false,
                tag = alimentationTag
            ),
            Transaction(
                id = UUID.randomUUID(),
                label = "Salaire",
                date = LocalDate.of(2025, 1, 15),
                amount = Amount(BigDecimal("2500.00")),
                isIncome = true,
                tag = null
            ),
            Transaction(
                id = UUID.randomUUID(),
                label = "Essence",
                date = LocalDate.of(2025, 1, 20),
                amount = Amount(BigDecimal("60.00")),
                isIncome = false,
                tag = transportTag
            )
        )

        val csvContent = exporter.exportToCsv(transactions)

        val lines = csvContent.split("\n")
        assertEquals(4, lines.size) // Header + 3 transactions
        assertEquals("date;label;depense;recette;tag", lines[0])
        assertEquals("10-01-2025;Courses;45,50;;Alimentation", lines[1])
        assertEquals("15-01-2025;Salaire;;2500,00;", lines[2])
        assertEquals("20-01-2025;Essence;60,00;;Transport", lines[3])
    }

    @Test
    fun `exportToCsv should escape labels containing semicolons`() {
        val transaction = Transaction(
            id = UUID.randomUUID(),
            label = "Achat; avec séparateur",
            date = LocalDate.of(2025, 1, 15),
            amount = Amount(BigDecimal("45.50")),
            isIncome = false
        )

        val csvContent = exporter.exportToCsv(listOf(transaction))

        val lines = csvContent.split("\n")
        assertEquals(2, lines.size)
        assertTrue(lines[1].contains("\"Achat; avec séparateur\""))
    }

    @Test
    fun `exportToCsv should escape labels containing quotes`() {
        val transaction = Transaction(
            id = UUID.randomUUID(),
            label = "Achat \"special\"",
            date = LocalDate.of(2025, 1, 15),
            amount = Amount(BigDecimal("45.50")),
            isIncome = false
        )

        val csvContent = exporter.exportToCsv(listOf(transaction))

        val lines = csvContent.split("\n")
        assertEquals(2, lines.size)
        assertTrue(lines[1].contains("\"Achat \"\"special\"\"\""))
    }

    @Test
    fun `exportToCsv should handle transactions without tags`() {
        val transaction = Transaction(
            id = UUID.randomUUID(),
            label = "Transaction sans tag",
            date = LocalDate.of(2025, 1, 15),
            amount = Amount(BigDecimal("45.50")),
            isIncome = false,
            tag = null
        )

        val csvContent = exporter.exportToCsv(listOf(transaction))

        val lines = csvContent.split("\n")
        assertEquals(2, lines.size)
        assertEquals("date;label;depense;recette;tag", lines[0])
        assertEquals("15-01-2025;Transaction sans tag;45,50;;", lines[1])
    }

    @Test
    fun `exportToCsv should format amounts with comma as decimal separator`() {
        val transactions = listOf(
            Transaction(
                id = UUID.randomUUID(),
                label = "Test 1",
                date = LocalDate.of(2025, 1, 15),
                amount = Amount(BigDecimal("123.45")),
                isIncome = false
            ),
            Transaction(
                id = UUID.randomUUID(),
                label = "Test 2",
                date = LocalDate.of(2025, 1, 16),
                amount = Amount(BigDecimal("1000.99")),
                isIncome = true
            )
        )

        val csvContent = exporter.exportToCsv(transactions)

        val lines = csvContent.split("\n")
        assertTrue(lines[1].contains("123,45"))
        assertTrue(lines[2].contains("1000,99"))
    }

    @Test
    fun `exportToCsv should format dates in dd-MM-yyyy format`() {
        val transaction = Transaction(
            id = UUID.randomUUID(),
            label = "Test",
            date = LocalDate.of(2025, 11, 4),
            amount = Amount(BigDecimal("100.00")),
            isIncome = false
        )

        val csvContent = exporter.exportToCsv(listOf(transaction))

        val lines = csvContent.split("\n")
        assertTrue(lines[1].startsWith("04-11-2025;"))
    }

    @Test
    fun `exportToCsv should return only header when all transactions are preview`() {
        val transactions = listOf(
            Transaction(
                id = UUID.randomUUID(),
                label = "Preview 1",
                date = LocalDate.of(2025, 1, 15),
                amount = Amount(BigDecimal("100.00")),
                isIncome = false,
                isPreview = true
            ),
            Transaction(
                id = UUID.randomUUID(),
                label = "Preview 2",
                date = LocalDate.of(2025, 1, 16),
                amount = Amount(BigDecimal("200.00")),
                isIncome = false,
                isPreview = true
            )
        )

        val csvContent = exporter.exportToCsv(transactions)

        assertEquals("date;label;depense;recette;tag", csvContent)
    }

    @Test
    fun `exportToCsv should use tag label property for export`() {
        val alimentationTag = defaultTags.find { it.label == "Alimentation & Restaurant" }

        val transaction = Transaction(
            id = UUID.randomUUID(),
            label = "Test",
            date = LocalDate.of(2025, 1, 15),
            amount = Amount(BigDecimal("45.50")),
            isIncome = false,
            tag = alimentationTag
        )

        val csvContent = exporter.exportToCsv(listOf(transaction))

        val lines = csvContent.split("\n")
        assertTrue(lines[1].endsWith(";Alimentation & Restaurant"))
    }
}

