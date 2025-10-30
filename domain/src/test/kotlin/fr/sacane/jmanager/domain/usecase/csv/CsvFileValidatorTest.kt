package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.awt.Color

@DisplayName("CsvFileValidator Tests")
class CsvFileValidatorTest {

    private lateinit var validator: CsvFileValidator
    private lateinit var availableTags: List<Tag>

    @BeforeEach
    fun setup() {
        validator = CsvFileValidator()
        availableTags = defaultTags + listOf(
            Tag(label = "CustomTag", id = null, isDefault = false, color = Color.BLUE)
        )
    }

    @Test
    fun `should fail when CSV file is empty`() {
        val rows = emptyList<Array<String>>()

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_EMPTY_FILE)
        assertEquals("CSV file is empty", result.message)
    }

    @Test
    fun `should fail when header has missing columns`() {
        val rows = listOf(
            arrayOf("date", "label", "depense")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_MISSING_COLUMNS)
        assertTrue(result.message.contains("Expected 5 columns but found 3"))
    }

    @Test
    fun `should fail when header has extra columns`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag", "extra")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_EXTRA_COLUMNS)
        assertTrue(result.message.contains("Expected 5 columns but found 6"))
    }

    @Test
    fun `should fail when header columns are in wrong order`() {
        val rows = listOf(
            arrayOf("label", "date", "depense", "recette", "tag")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_INVALID_HEADER)
        assertTrue(result.message.contains("Invalid header format"))
    }

    @Test
    fun `should fail when header has wrong column names`() {
        val rows = listOf(
            arrayOf("date", "description", "expense", "income", "category")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_INVALID_HEADER)
        assertTrue(result.message.contains("Invalid header format"))
    }

    @Test
    fun `should succeed with valid header and no data rows`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertEquals(0, report.totalLines)
            assertEquals(0, report.validLines)
            assertEquals(0, report.warnings.size)
        }
    }

    @Test
    fun `should succeed with valid CSV file`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "45.50", "", "Alimentation & Restaurant"),
            arrayOf("16-01-2025", "Salary", "", "2500.00", "Aucune")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertEquals(2, report.totalLines)
            assertEquals(2, report.validLines)
            assertEquals(0, report.warnings.size)
            assertEquals(0, report.suggestions.size)
        }
    }

    @Test
    fun `should fail when data line has malformed columns`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "45.50")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_MALFORMED_LINE)
        assertTrue(result.message.contains("Line 2 has 3 columns instead of 5"))
    }

    @Test
    fun `should fail when date is missing`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("", "Groceries", "45.50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_MISSING_REQUIRED_FIELD)
        assertTrue(result.message.contains("Line 2: Date is required"))
    }

    @Test
    fun `should fail when date format is invalid`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("2025-01-15", "Groceries", "45.50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_INVALID_DATE_FORMAT)
        assertTrue(result.message.contains("Line 2: Invalid date format"))
        assertTrue(result.message.contains("dd-MM-yyyy"))
    }

    @Test
    fun `should fail when label is missing`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "", "45.50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_MISSING_REQUIRED_FIELD)
        assertTrue(result.message.contains("Line 2: Label is required"))
    }

    @Test
    fun `should fail when label exceeds max length`() {
        val longLabel = "a".repeat(201)
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", longLabel, "45.50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_MISSING_REQUIRED_FIELD)
        assertTrue(result.message.contains("Label exceeds maximum length"))
    }

    @Test
    fun `should fail when both depense and recette are empty`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_NO_AMOUNT_FILLED)
        assertTrue(result.message.contains("Either 'depense' or 'recette' must be filled"))
    }

    @Test
    fun `should fail when both depense and recette are filled`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "45.50", "100.00", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_BOTH_AMOUNTS_FILLED)
        assertTrue(result.message.contains("Only one of 'depense' or 'recette' should be filled"))
    }

    @Test
    fun `should fail when amount format is invalid`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "abc", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_INVALID_AMOUNT_FORMAT)
        assertTrue(result.message.contains("Invalid amount format"))
    }

    @Test
    fun `should fail when amount is negative`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "-45.50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_NEGATIVE_AMOUNT)
        assertTrue(result.message.contains("Amount cannot be negative"))
    }

    @Test
    fun `should accept comma as decimal separator`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "45,50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertEquals(1, report.validLines)
        }
    }

    @Test
    fun `should succeed with warning when tag is unknown`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "45.50", "", "UnknownTag")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertEquals(1, report.totalLines)
            assertEquals(1, report.validLines)
            assertEquals(1, report.warnings.size)
            assertEquals(2, report.warnings[0].lineNumber)
            assertTrue(report.warnings[0].message.contains("Tag 'UnknownTag' not found"))
        }
    }

    @Test
    fun `should succeed when tag is empty`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "45.50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertEquals(1, report.validLines)
            assertEquals(0, report.warnings.size)
        }
    }

    @Test
    fun `should recognize known tag case-insensitively`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "45.50", "", "alimentation & restaurant")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertEquals(0, report.warnings.size)
        }
    }

    @Test
    fun `should fail with column swap detection - date column contains amount`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("45.50", "Groceries", "15-01-2025", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_POSSIBLE_COLUMN_SWAP)
        assertTrue(result.message.contains("Date column contains what looks like an amount"))
    }

    @Test
    fun `should fail with column swap detection - label column contains number`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "45.50", "Groceries", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_POSSIBLE_COLUMN_SWAP)
        assertTrue(result.message.contains("Label looks like a numeric value"))
    }

    @Test
    fun `should succeed with warning when amount column looks like text`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "45.50", "", ""),
            arrayOf("16-01-2025", "Transport", "some text value", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_INVALID_AMOUNT_FORMAT)
    }

    @Test
    fun `should provide suggestions when multiple lines have swapped columns`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "45.50", "30", "", ""),
            arrayOf("16-01-2025", "100.00", "50", "", ""),
            arrayOf("17-01-2025", "75.25", "25", "", ""),
            arrayOf("18-01-2025", "200.00", "100", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure()
    }

    @Test
    fun `should process multiple valid lines successfully`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "45.50", "", "Alimentation & Restaurant"),
            arrayOf("16-01-2025", "Salary", "", "2500.00", "Aucune"),
            arrayOf("17-01-2025", "Transport", "15.00", "", "Transport"),
            arrayOf("18-01-2025", "Restaurant", "50.00", "", "Alimentation & Restaurant")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertEquals(4, report.totalLines)
            assertEquals(4, report.validLines)
            assertEquals(0, report.warnings.size)
        }
    }

    @Test
    fun `should stop at first critical error`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Valid line", "45.50", "", ""),
            arrayOf("invalid-date", "Second line", "30.00", "", ""),
            arrayOf("17-01-2025", "Third line", "20.00", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure(ResultState.CSV_INVALID_DATE_FORMAT)
        assertTrue(result.message.contains("Line 3"))
    }

    @Test
    fun `should handle whitespace in values correctly`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("  15-01-2025  ", "  Groceries  ", "  45.50  ", "", "  Alimentation & Restaurant  ")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertEquals(1, report.validLines)
        }
    }

    @Test
    fun `should handle mixed valid and warning cases`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "45.50", "", "UnknownTag1"),
            arrayOf("16-01-2025", "Salary", "", "2500.00", "Aucune"),
            arrayOf("17-01-2025", "Transport", "15.00", "", "UnknownTag2")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertEquals(3, report.totalLines)
            assertEquals(3, report.validLines)
            assertEquals(2, report.warnings.size)
            assertEquals(2, report.warnings[0].lineNumber)
            assertEquals(4, report.warnings[1].lineNumber)
        }
    }

    @Test
    fun `should detect column swap pattern with heuristic`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "123.45", "30.00", "", ""),
            arrayOf("16-01-2025", "567.89", "50.00", "", ""),
            arrayOf("17-01-2025", "111.11", "25.00", "", ""),
            arrayOf("18-01-2025", "222.22", "60.00", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertFailure()
    }

    @Test
    fun `should handle header case insensitively`() {
        val rows = listOf(
            arrayOf("Date", "Label", "Depense", "Recette", "Tag"),
            arrayOf("15-01-2025", "Groceries", "45.50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
    }

    @Test
    fun `should validate recette column when depense is empty`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Salary", "", "2500.50", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertEquals(1, report.validLines)
        }
    }

    @Test
    fun `should accept very small amounts`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Small purchase", "0.01", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
    }

    @Test
    fun `should accept zero as amount`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Zero amount", "0", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
    }

    @Test
    fun `should accept large amounts`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Large purchase", "999999.99", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
    }
}

