package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.csv.CsvReportType
import fr.sacane.jmanager.domain.models.defaultTags
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
    fun `should return error when CSV file is empty`() {
        val rows = emptyList<Array<String>>()

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertFalse(report.canImport)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.EMPTY_FILE, report.errors[0].type)
        }
    }

    @Test
    fun `should return error when header has missing columns`() {
        val rows = listOf(
            arrayOf("date", "label", "depense")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.MISSING_COLUMNS, report.errors[0].type)
        }
    }

    @Test
    fun `should return error when header has extra columns`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag", "extra")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.EXTRA_COLUMNS, report.errors[0].type)
        }
    }

    @Test
    fun `should return error when header columns are in wrong order`() {
        val rows = listOf(
            arrayOf("label", "date", "depense", "recette", "tag")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.INVALID_HEADER, report.errors[0].type)
        }
    }

    @Test
    fun `should succeed with valid header and no data rows`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertFalse(report.hasErrors)
            assertTrue(report.canImport)
            assertEquals(0, report.totalLines)
            assertEquals(0, report.validLines)
            assertEquals(0, report.errors.size)
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
            assertFalse(report.hasErrors)
            assertTrue(report.canImport)
            assertEquals(2, report.totalLines)
            assertEquals(2, report.validLines)
            assertEquals(0, report.errors.size)
            assertEquals(0, report.warnings.size)
        }
    }

    @Test
    fun `should return error when data line has malformed columns`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "45.50")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.MALFORMED_LINE, report.errors[0].type)
        }
    }

    @Test
    fun `should return error when date is missing`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("", "Groceries", "45.50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.MISSING_REQUIRED_FIELD, report.errors[0].type)
        }
    }

    @Test
    fun `should return error when date format is invalid`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("2025-01-15", "Groceries", "45.50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.INVALID_DATE_FORMAT, report.errors[0].type)
        }
    }

    @Test
    fun `should return error when label is missing`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "", "45.50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.MISSING_REQUIRED_FIELD, report.errors[0].type)
        }
    }

    @Test
    fun `should return error when both depense and recette are empty`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.NO_AMOUNT_FILLED, report.errors[0].type)
        }
    }

    @Test
    fun `should return error when both depense and recette are filled`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "45.50", "100.00", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.BOTH_AMOUNTS_FILLED, report.errors[0].type)
        }
    }

    @Test
    fun `should return error when amount format is invalid`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "abc", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.INVALID_AMOUNT_FORMAT, report.errors[0].type)
        }
    }

    @Test
    fun `should return error when amount is negative`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Groceries", "-45.50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.NEGATIVE_AMOUNT, report.errors[0].type)
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
            assertFalse(report.hasErrors)
            assertTrue(report.canImport)
            assertEquals(1, report.totalLines)
            assertEquals(1, report.validLines)
            assertEquals(0, report.errors.size)
            assertEquals(1, report.warnings.size)
            assertEquals(CsvReportType.UNKNOWN_TAG, report.warnings[0].type)
        }
    }

    @Test
    fun `should stop at first critical error per line`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "Valid line", "45.50", "", ""),
            arrayOf("invalid-date", "Second line", "30.00", "", ""),
            arrayOf("17-01-2025", "Third line", "20.00", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(2, report.validLines)
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
            assertFalse(report.hasErrors)
            assertTrue(report.canImport)
            assertEquals(3, report.totalLines)
            assertEquals(3, report.validLines)
            assertEquals(0, report.errors.size)
            assertEquals(2, report.warnings.size)
        }
    }

    @Test
    fun `should return error for column swap detection - date column contains amount`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("45.50", "Groceries", "15-01-2025", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.POSSIBLE_COLUMN_SWAP, report.errors[0].type)
        }
    }

    @Test
    fun `should return error for column swap detection - label column contains number`() {
        val rows = listOf(
            arrayOf("date", "label", "depense", "recette", "tag"),
            arrayOf("15-01-2025", "45.50", "Groceries", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.POSSIBLE_COLUMN_SWAP, report.errors[0].type)
        }
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
            assertFalse(report.hasErrors)
            assertEquals(1, report.validLines)
        }
    }

    @Test
    fun `should handle header case insensitively`() {
        val rows = listOf(
            arrayOf("Date", "Label", "Depense", "Recette", "Tag"),
            arrayOf("15-01-2025", "Groceries", "45.50", "", "")
        )

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertFalse(report.hasErrors)
        }
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
            assertFalse(report.hasErrors)
            assertEquals(4, report.totalLines)
            assertEquals(4, report.validLines)
            assertEquals(0, report.errors.size)
        }
    }

    @Test
    @DisplayName("Should reject CSV files with more than 10,000 rows")
    fun `should reject CSV files exceeding 10000 rows limit`() {
        // Créer un header + 10,001 lignes de données (dépasse la limite)
        val header = arrayOf("date", "label", "depense", "recette", "tag")
        val dataLine = arrayOf("15-01-2025", "Transaction", "45.50", "", "Aucune")

        val rows = mutableListOf(header)
        repeat(10001) {
            rows.add(dataLine)
        }

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertTrue(report.hasErrors)
            assertFalse(report.canImport)
            assertEquals(1, report.errors.size)
            assertEquals(CsvReportType.TOO_MANY_ROWS, report.errors[0].type)
            assertTrue(report.errors[0].message.contains("10001"))
            assertTrue(report.errors[0].message.contains("10000"))
        }
    }

    @Test
    @DisplayName("Should accept CSV files with exactly 10,000 rows")
    fun `should accept CSV files with exactly 10000 rows`() {
        // Créer un header + exactement 10,000 lignes (limite exacte)
        val header = arrayOf("date", "label", "depense", "recette", "tag")
        val dataLine = arrayOf("15-01-2025", "Transaction", "45.50", "", "Aucune")

        val rows = mutableListOf(header)
        repeat(10000) {
            rows.add(dataLine)
        }

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertFalse(report.hasErrors)
            assertTrue(report.canImport)
            assertEquals(10000, report.totalLines)
            assertEquals(0, report.errors.size)
        }
    }

    @Test
    @DisplayName("Should accept CSV files with less than 10,000 rows")
    fun `should accept CSV files with 9999 rows`() {
        // Créer un header + 9,999 lignes (sous la limite)
        val header = arrayOf("date", "label", "depense", "recette", "tag")
        val dataLine = arrayOf("15-01-2025", "Transaction", "45.50", "", "Aucune")

        val rows = mutableListOf(header)
        repeat(9999) {
            rows.add(dataLine)
        }

        val result = validator.validate(rows, availableTags)

        result.assertSuccess()
        result.onSuccess { report ->
            assertFalse(report.hasErrors)
            assertTrue(report.canImport)
            assertEquals(9999, report.totalLines)
            assertEquals(9999, report.validLines)
        }
    }
}

