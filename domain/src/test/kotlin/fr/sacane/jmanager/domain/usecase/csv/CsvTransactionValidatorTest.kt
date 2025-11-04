
package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.csv.CsvLineResult
import fr.sacane.jmanager.domain.models.csv.CsvTransactionLine
import fr.sacane.jmanager.domain.models.defaultTags
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class CsvTransactionValidatorTest {

    private lateinit var validator: CsvTransactionValidator
    private lateinit var availableTags: List<Tag>

    @BeforeEach
    fun setup() {
        validator = CsvTransactionValidator()
        availableTags = defaultTags
    }

    @Test
    fun `validateAndConvert should succeed with valid expense line`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "15-01-2025",
            label = "Courses alimentaires",
            depense = "45.50",
            recette = "",
            tag = "Alimentation & Restaurant"
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Success)
        val transaction = (result as CsvLineResult.Success).transaction
        assertEquals("Courses alimentaires", transaction.label)
        assertEquals(LocalDate.of(2025, 1, 15), transaction.date)
        assertEquals(BigDecimal("45.50"), transaction.amount.value)
        assertFalse(transaction.isIncome)
        assertEquals("Alimentation & Restaurant", transaction.tag?.label)
    }

    @Test
    fun `validateAndConvert should succeed with valid income line`() {
        val line = CsvTransactionLine(
            lineNumber = 3,
            date = "20-01-2025",
            label = "Salaire",
            depense = "",
            recette = "2500.00",
            tag = "Aucune"
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Success)
        val transaction = (result as CsvLineResult.Success).transaction
        assertEquals("Salaire", transaction.label)
        assertEquals(LocalDate.of(2025, 1, 20), transaction.date)
        assertEquals(BigDecimal("2500.00"), transaction.amount.value)
        assertTrue(transaction.isIncome)
    }

    @Test
    fun `validateAndConvert should accept comma as decimal separator`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "15-01-2025",
            label = "Achat",
            depense = "123,45",
            recette = "",
            tag = ""
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Success)
        val transaction = (result as CsvLineResult.Success).transaction
        assertEquals(BigDecimal("123.45"), transaction.amount.value)
    }

    @Test
    fun `validateAndConvert should fail when date is empty`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "",
            label = "Test",
            depense = "10.00",
            recette = "",
            tag = ""
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Error)
        val error = result as CsvLineResult.Error
        assertEquals(2, error.lineNumber)
        assertTrue(error.errors.any { it.contains("date est obligatoire") })
    }

    @Test
    fun `validateAndConvert should fail when date format is invalid`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "2025-01-15",
            label = "Test",
            depense = "10.00",
            recette = "",
            tag = ""
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Error)
        val error = result as CsvLineResult.Error
        assertTrue(error.errors.any { it.contains("Format de date invalide") })
        assertTrue(error.errors.any { it.contains("jj-MM-aaaa") })
    }

    @Test
    fun `validateAndConvert should fail when label is empty`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "15-01-2025",
            label = "",
            depense = "10.00",
            recette = "",
            tag = ""
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Error)
        val error = result as CsvLineResult.Error
        assertTrue(error.errors.any { it.contains("libellé est obligatoire") })
    }

    @Test
    fun `validateAndConvert should fail when label is too long`() {
        val longLabel = "a".repeat(201)
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "15-01-2025",
            label = longLabel,
            depense = "10.00",
            recette = "",
            tag = ""
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Error)
        val error = result as CsvLineResult.Error
        assertTrue(error.errors.any { it.contains("libellé ne peut pas dépasser") })
    }

    @Test
    fun `validateAndConvert should fail when both depense and recette are empty`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "15-01-2025",
            label = "Test",
            depense = "",
            recette = "",
            tag = ""
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Error)
        val error = result as CsvLineResult.Error
        assertTrue(error.errors.any { it.contains("soit une dépense soit une recette") })
    }

    @Test
    fun `validateAndConvert should fail when both depense and recette are filled`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "15-01-2025",
            label = "Test",
            depense = "10.00",
            recette = "20.00",
            tag = ""
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Error)
        val error = result as CsvLineResult.Error
        assertTrue(error.errors.any { it.contains("ne pouvez pas renseigner à la fois") })
    }

    @Test
    fun `validateAndConvert should fail when amount format is invalid`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "15-01-2025",
            label = "Test",
            depense = "abc",
            recette = "",
            tag = ""
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Error)
        val error = result as CsvLineResult.Error
        assertTrue(error.errors.any { it.contains("Format de montant invalide") })
    }

    @Test
    fun `validateAndConvert should fail when amount is negative`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "15-01-2025",
            label = "Test",
            depense = "-10.00",
            recette = "",
            tag = ""
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Error)
        val error = result as CsvLineResult.Error
        assertTrue(error.errors.any { it.contains("ne peut pas être négatif") })
    }

    @Test
    fun `validateAndConvert should use default tag when tag is not found`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "15-01-2025",
            label = "Test",
            depense = "10.00",
            recette = "",
            tag = "TagInexistant"
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Success)
        val transaction = (result as CsvLineResult.Success).transaction
        assertEquals("Aucune", transaction.tag?.label)
    }

    @Test
    fun `validateAndConvert should use noneTag when tag is empty`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "15-01-2025",
            label = "Test",
            depense = "10.00",
            recette = "",
            tag = ""
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Success)
        val transaction = (result as CsvLineResult.Success).transaction
        assertEquals("Aucune", transaction.tag?.label)
    }

    @Test
    fun `validateAndConvert should be case insensitive for tag matching`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "15-01-2025",
            label = "Test",
            depense = "10.00",
            recette = "",
            tag = "ALIMENTATION & RESTAURANT"
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Success)
        val transaction = (result as CsvLineResult.Success).transaction
        assertEquals("Alimentation & Restaurant", transaction.tag?.label)
    }

    @Test
    fun `validateAndConvert should trim whitespace from all fields`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "  15-01-2025  ",
            label = "  Test  ",
            depense = "  10.00  ",
            recette = "",
            tag = "  Santé  "
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Success)
        val transaction = (result as CsvLineResult.Success).transaction
        assertEquals("Test", transaction.label)
        assertEquals(LocalDate.of(2025, 1, 15), transaction.date)
        assertEquals(BigDecimal("10.00"), transaction.amount.value)
        assertEquals("Santé", transaction.tag?.label)
    }

    @Test
    fun `validateAndConvert should return multiple errors when multiple fields are invalid`() {
        val line = CsvTransactionLine(
            lineNumber = 2,
            date = "invalid-date",
            label = "",
            depense = "",
            recette = "",
            tag = ""
        )

        val result = validator.validateAndConvert(line, availableTags)

        assertTrue(result is CsvLineResult.Error)
        val error = result as CsvLineResult.Error
        assertTrue(error.errors.size >= 2)
        assertTrue(error.errors.any { it.contains("date") })
        assertTrue(error.errors.any { it.contains("libellé") })
    }
}