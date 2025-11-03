package fr.sacane.jmanager.domain.usecase.csv

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDate

@DisplayName("CsvValidationUtils Date Parsing Tests")
class CsvValidationUtilsDateTest {

    @Test
    @DisplayName("Should parse full date format dd-MM-yyyy")
    fun `parseDate should parse full date format`() {
        val result = CsvValidationUtils.parseDate("15-01-2026")
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 1, 15), result)
    }

    @Test
    @DisplayName("Should parse day only when month and year provided")
    fun `parseDate should parse day only with month and year`() {
        val result = CsvValidationUtils.parseDate("15", month = 1, year = 2026)
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 1, 15), result)
    }

    @Test
    @DisplayName("Should return null for day only without month and year")
    fun `parseDate should return null for day only without month year`() {
        val result = CsvValidationUtils.parseDate("15")
        assertNull(result)
    }

    @Test
    @DisplayName("Should prioritize full date format over day only")
    fun `parseDate should prioritize full date format`() {
        // Even if month/year provided, full date should be used
        val result = CsvValidationUtils.parseDate("15-02-2026", month = 1, year = 2025)
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 2, 15), result)
    }

    @Test
    @DisplayName("Should handle day 1")
    fun `parseDate should handle day 1`() {
        val result = CsvValidationUtils.parseDate("1", month = 1, year = 2026)
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 1, 1), result)
    }

    @Test
    @DisplayName("Should return null for invalid day 32")
    fun `parseDate should return null for invalid day 32`() {
        val result = CsvValidationUtils.parseDate("32", month = 1, year = 2026)
        assertNull(result)
    }

    @Test
    @DisplayName("Should return null for invalid day 0")
    fun `parseDate should return null for invalid day 0`() {
        val result = CsvValidationUtils.parseDate("0", month = 1, year = 2026)
        assertNull(result)
    }
}

