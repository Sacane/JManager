package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.defaultTags
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@DisplayName("CsvValidationUtils Tests")
class CsvValidationUtilsTest {

    @Nested
    @DisplayName("looksLikeAmount")
    inner class LooksLikeAmountTests {

        @Test
        fun `should return true for valid integer amounts`() {
            assertTrue(CsvValidationUtils.looksLikeAmount("100"))
            assertTrue(CsvValidationUtils.looksLikeAmount("0"))
            assertTrue(CsvValidationUtils.looksLikeAmount("1"))
            assertTrue(CsvValidationUtils.looksLikeAmount("123456"))
        }

        @Test
        fun `should return true for valid decimal amounts with dot`() {
            assertTrue(CsvValidationUtils.looksLikeAmount("100.50"))
            assertTrue(CsvValidationUtils.looksLikeAmount("0.99"))
            assertTrue(CsvValidationUtils.looksLikeAmount("123.45"))
        }

        @Test
        fun `should return true for valid decimal amounts with comma`() {
            assertTrue(CsvValidationUtils.looksLikeAmount("100,50"))
            assertTrue(CsvValidationUtils.looksLikeAmount("0,99"))
            assertTrue(CsvValidationUtils.looksLikeAmount("123,45"))
        }

        @Test
        fun `should return true for amounts with leading or trailing spaces`() {
            assertTrue(CsvValidationUtils.looksLikeAmount(" 100.50 "))
            assertTrue(CsvValidationUtils.looksLikeAmount("  100  "))
        }

        @Test
        fun `should return true for negative amounts`() {
            assertTrue(CsvValidationUtils.looksLikeAmount("-100"))
            assertTrue(CsvValidationUtils.looksLikeAmount("-100.50"))
            assertTrue(CsvValidationUtils.looksLikeAmount("-100,50"))
        }

        @Test
        fun `should return false for empty or blank strings`() {
            assertFalse(CsvValidationUtils.looksLikeAmount(""))
            assertFalse(CsvValidationUtils.looksLikeAmount("   "))
        }

        @Test
        fun `should return false for text strings`() {
            assertFalse(CsvValidationUtils.looksLikeAmount("abc"))
            assertFalse(CsvValidationUtils.looksLikeAmount("Courses au supermarché"))
            assertFalse(CsvValidationUtils.looksLikeAmount("100€"))
        }

        @Test
        fun `should return false for invalid number formats`() {
            assertFalse(CsvValidationUtils.looksLikeAmount("100.50.25"))
            assertFalse(CsvValidationUtils.looksLikeAmount("100,50,25"))
            assertFalse(CsvValidationUtils.looksLikeAmount("abc123"))
        }
    }

    @Nested
    @DisplayName("looksLikeText")
    inner class LooksLikeTextTests {

        @Test
        fun `should return true for text with multiple words`() {
            assertTrue(CsvValidationUtils.looksLikeText("Courses au supermarché"))
            assertTrue(CsvValidationUtils.looksLikeText("Achat de vêtements"))
            assertTrue(CsvValidationUtils.looksLikeText("Salaire mensuel"))
        }

        @Test
        fun `should return true for long single words`() {
            assertTrue(CsvValidationUtils.looksLikeText("Restaurant"))
            assertTrue(CsvValidationUtils.looksLikeText("Transport"))
            assertTrue(CsvValidationUtils.looksLikeText("Supermarché"))
        }

        @Test
        fun `should return false for short single words`() {
            assertFalse(CsvValidationUtils.looksLikeText("abc"))
            assertFalse(CsvValidationUtils.looksLikeText("test"))
            assertFalse(CsvValidationUtils.looksLikeText("ok"))
        }

        @Test
        fun `should return false for numbers`() {
            assertFalse(CsvValidationUtils.looksLikeText("123"))
            assertFalse(CsvValidationUtils.looksLikeText("100.50"))
            assertFalse(CsvValidationUtils.looksLikeText("100,50"))
        }

        @Test
        fun `should return false for empty or blank strings`() {
            assertFalse(CsvValidationUtils.looksLikeText(""))
            assertFalse(CsvValidationUtils.looksLikeText("   "))
        }

        @Test
        fun `should return false for strings without letters`() {
            assertFalse(CsvValidationUtils.looksLikeText("123 456"))
            assertFalse(CsvValidationUtils.looksLikeText("!@# $%^"))
        }
    }

    @Nested
    @DisplayName("parseAmountValue")
    inner class ParseAmountValueTests {

        @Test
        fun `should parse valid integer amounts`() {
            assertEquals(BigDecimal("100"), CsvValidationUtils.parseAmountValue("100"))
            assertEquals(BigDecimal("0"), CsvValidationUtils.parseAmountValue("0"))
            assertEquals(BigDecimal("123456"), CsvValidationUtils.parseAmountValue("123456"))
        }

        @Test
        fun `should parse valid decimal amounts with dot`() {
            assertEquals(BigDecimal("100.50"), CsvValidationUtils.parseAmountValue("100.50"))
            assertEquals(BigDecimal("0.99"), CsvValidationUtils.parseAmountValue("0.99"))
        }

        @Test
        fun `should parse valid decimal amounts with comma`() {
            assertEquals(BigDecimal("100.50"), CsvValidationUtils.parseAmountValue("100,50"))
            assertEquals(BigDecimal("0.99"), CsvValidationUtils.parseAmountValue("0,99"))
        }

        @Test
        fun `should parse amounts with leading or trailing spaces`() {
            assertEquals(BigDecimal("100.50"), CsvValidationUtils.parseAmountValue(" 100.50 "))
            assertEquals(BigDecimal("100"), CsvValidationUtils.parseAmountValue("  100  "))
        }

        @Test
        fun `should parse negative amounts`() {
            assertEquals(BigDecimal("-100"), CsvValidationUtils.parseAmountValue("-100"))
            assertEquals(BigDecimal("-100.50"), CsvValidationUtils.parseAmountValue("-100.50"))
        }

        @Test
        fun `should return null for invalid amounts`() {
            assertNull(CsvValidationUtils.parseAmountValue("abc"))
            assertNull(CsvValidationUtils.parseAmountValue("100.50.25"))
            assertNull(CsvValidationUtils.parseAmountValue(""))
        }
    }

    @Nested
    @DisplayName("parseAmount")
    inner class ParseAmountTests {

        @Test
        fun `should parse valid amounts and return Amount object`() {
            val errors = mutableListOf<String>()
            val amount = CsvValidationUtils.parseAmount("100.50", errors, "dépense")

            assertNotNull(amount)
            assertEquals(BigDecimal("100.50"), amount!!.value)
            assertTrue(errors.isEmpty())
        }

        @Test
        fun `should parse amounts with comma separator`() {
            val errors = mutableListOf<String>()
            val amount = CsvValidationUtils.parseAmount("100,50", errors, "recette")

            assertNotNull(amount)
            assertEquals(BigDecimal("100.50"), amount!!.value)
            assertTrue(errors.isEmpty())
        }

        @Test
        fun `should add error for invalid amount format`() {
            val errors = mutableListOf<String>()
            val amount = CsvValidationUtils.parseAmount("abc", errors, "dépense")

            assertNull(amount)
            assertEquals(1, errors.size)
            assertTrue(errors[0].contains("Format de montant invalide"))
            assertTrue(errors[0].contains("dépense"))
        }

        @Test
        fun `should add error for negative amounts`() {
            val errors = mutableListOf<String>()
            val amount = CsvValidationUtils.parseAmount("-100", errors, "recette")

            assertNull(amount)
            assertEquals(1, errors.size)
            assertTrue(errors[0].contains("ne peut pas être négatif"))
        }

        @Test
        fun `should accept zero as valid amount`() {
            val errors = mutableListOf<String>()
            val amount = CsvValidationUtils.parseAmount("0", errors, "dépense")

            assertNotNull(amount)
            assertEquals(0, amount!!.value.compareTo(BigDecimal.ZERO))
            assertTrue(errors.isEmpty())
        }

        @Test
        fun `should handle amounts with spaces`() {
            val errors = mutableListOf<String>()
            val amount = CsvValidationUtils.parseAmount("  100.50  ", errors, "dépense")

            assertNotNull(amount)
            assertEquals(BigDecimal("100.50"), amount!!.value)
            assertTrue(errors.isEmpty())
        }
    }

    @Nested
    @DisplayName("validateTag")
    inner class ValidateTagTests {

        private val customTag1 = Tag("CustomTag1", UUID.randomUUID())
        private val customTag2 = Tag("CustomTag2", UUID.randomUUID())
        private val availableTags = listOf(customTag1, customTag2)

        @Test
        fun `should return noneTag for blank tag string`() {
            val tag = CsvValidationUtils.validateTag("", availableTags)
            assertEquals(Tag.noneTag().label, tag.label)
            assertEquals(Tag.noneTag().isDefault, tag.isDefault)
        }

        @Test
        fun `should return noneTag for whitespace-only tag string`() {
            val tag = CsvValidationUtils.validateTag("   ", availableTags)
            assertEquals(Tag.noneTag().label, tag.label)
            assertEquals(Tag.noneTag().isDefault, tag.isDefault)
        }

        @Test
        fun `should find tag in available tags (case insensitive)`() {
            val tag = CsvValidationUtils.validateTag("CustomTag1", availableTags)
            assertSame(customTag1, tag)
        }

        @Test
        fun `should find tag in available tags with different case`() {
            val tag = CsvValidationUtils.validateTag("customtag1", availableTags)
            assertSame(customTag1, tag)
        }

        @Test
        fun `should find tag in default tags when not in available tags`() {
            val defaultTagLabel = defaultTags.first().label
            val tag = CsvValidationUtils.validateTag(defaultTagLabel, availableTags)
            assertNotNull(tag)
            assertEquals(defaultTagLabel, tag.label)
        }

        @Test
        fun `should return noneTag when tag not found anywhere`() {
            val tag = CsvValidationUtils.validateTag("NonExistentTag", availableTags)
            assertEquals(Tag.noneTag().label, tag.label)
            assertEquals(Tag.noneTag().isDefault, tag.isDefault)
        }

        @Test
        fun `should trim whitespace from tag name before matching`() {
            val tag = CsvValidationUtils.validateTag("  CustomTag1  ", availableTags)
            assertSame(customTag1, tag)
        }

        @Test
        fun `should prioritize available tags over default tags`() {
            val defaultTagLabel = if (defaultTags.isNotEmpty()) defaultTags.first().label else "Transport"
            val customTagWithDefaultLabel = Tag(defaultTagLabel, UUID.randomUUID())
            val tagsWithOverride = listOf(customTagWithDefaultLabel)

            val tag = CsvValidationUtils.validateTag(defaultTagLabel, tagsWithOverride)
            assertEquals(customTagWithDefaultLabel.id, tag.id)
        }
    }

    @Nested
    @DisplayName("parseDate")
    inner class ParseDateTests {

        @Test
        fun `should parse valid date in dd-MM-yyyy format`() {
            val date = CsvValidationUtils.parseDate("15-03-2024")
            assertNotNull(date)
            assertEquals(LocalDate.of(2024, 3, 15), date)
        }

        @Test
        fun `should parse date with leading zeros`() {
            val date = CsvValidationUtils.parseDate("01-01-2024")
            assertNotNull(date)
            assertEquals(LocalDate.of(2024, 1, 1), date)
        }

        @Test
        fun `should parse date with spaces trimmed`() {
            val date = CsvValidationUtils.parseDate("  15-03-2024  ")
            assertNotNull(date)
            assertEquals(LocalDate.of(2024, 3, 15), date)
        }

        @Test
        fun `should return null for invalid date format`() {
            assertNull(CsvValidationUtils.parseDate("2024-03-15"))
            assertNull(CsvValidationUtils.parseDate("15/03/2024"))
            assertNull(CsvValidationUtils.parseDate("15-3-2024"))
        }

        @Test
        fun `should return null for invalid date values`() {
            assertNull(CsvValidationUtils.parseDate("32-01-2024"))
            assertNull(CsvValidationUtils.parseDate("15-13-2024"))
        }

        @Test
        fun `should return null for empty or blank strings`() {
            assertNull(CsvValidationUtils.parseDate(""))
            assertNull(CsvValidationUtils.parseDate("   "))
        }

        @Test
        fun `should return null for non-date strings`() {
            assertNull(CsvValidationUtils.parseDate("abc"))
            assertNull(CsvValidationUtils.parseDate("invalid date"))
        }

        @Test
        fun `should parse leap year date correctly`() {
            val date = CsvValidationUtils.parseDate("29-02-2024")
            assertNotNull(date)
            assertEquals(LocalDate.of(2024, 2, 29), date)
        }
    }

    @Nested
    @DisplayName("DATE_FORMATTER")
    inner class DateFormatterTests {

        @Test
        fun `DATE_FORMATTER should use dd-MM-yyyy pattern`() {
            val date = LocalDate.of(2024, 3, 15)
            val formatted = CsvValidationUtils.DATE_FORMATTER.format(date)
            assertEquals("15-03-2024", formatted)
        }

        @Test
        fun `DATE_FORMATTER should format single digit days with leading zero`() {
            val date = LocalDate.of(2024, 3, 5)
            val formatted = CsvValidationUtils.DATE_FORMATTER.format(date)
            assertEquals("05-03-2024", formatted)
        }

        @Test
        fun `DATE_FORMATTER should format single digit months with leading zero`() {
            val date = LocalDate.of(2024, 1, 15)
            val formatted = CsvValidationUtils.DATE_FORMATTER.format(date)
            assertEquals("15-01-2024", formatted)
        }
    }
}

