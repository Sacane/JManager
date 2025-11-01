package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.defaultTags
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Utility class containing shared validation methods for CSV processing
 */
object CsvValidationUtils {

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    /**
     * Checks if a string looks like a numeric amount
     */
    fun looksLikeAmount(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return false

        val normalized = trimmed.replace(',', '.')
        return try {
            BigDecimal(normalized)
            true
        } catch (_: NumberFormatException) {
            false
        }
    }

    /**
     * Checks if a string looks like text (contains letters and possibly multiple words)
     */
    fun looksLikeText(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return false

        val hasLetters = trimmed.any { it.isLetter() }
        val hasMultipleWords = trimmed.contains(' ')

        return hasLetters && (hasMultipleWords || trimmed.length > 5)
    }

    /**
     * Parses a string to a BigDecimal amount, handling comma/dot as decimal separator
     * Returns null if parsing fails
     */
    fun parseAmountValue(amountStr: String): BigDecimal? {
        return try {
            val normalized = amountStr.trim().replace(',', '.')
            BigDecimal(normalized)
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * Parses a string to an Amount object with validation
     * Adds error messages to the provided list if validation fails
     */
    fun parseAmount(amountStr: String, errors: MutableList<String>, fieldName: String): Amount? {
        val value = parseAmountValue(amountStr)

        if (value == null) {
            errors.add("Format de montant invalide pour la $fieldName. Utilisez des nombres avec point ou virgule (exemple: 123.45 ou 123,45)")
            return null
        }

        if (value < BigDecimal.ZERO) {
            errors.add("Le montant de la $fieldName ne peut pas être négatif")
            return null
        }

        return Amount(value)
    }

    /**
     * Validates and resolves a tag from available tags or default tags
     * Returns Tag.noneTag() if tag is blank or not found
     */
    fun validateTag(tagStr: String, availableTags: List<Tag>): Tag {
        if (tagStr.isBlank()) {
            return Tag.noneTag()
        }

        val trimmedTag = tagStr.trim()

        // Check in available tags first
        val matchingTag = availableTags.firstOrNull {
            it.label.equals(trimmedTag, ignoreCase = true)
        }

        if (matchingTag != null) {
            return matchingTag
        }

        // Check in default tags
        val defaultTag = defaultTags.firstOrNull {
            it.label.equals(trimmedTag, ignoreCase = true)
        }

        return defaultTag ?: Tag.noneTag()
    }

    /**
     * Parses a date string in dd-MM-yyyy format
     * Returns null if parsing fails
     */
    fun parseDate(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr.trim(), DATE_FORMATTER)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

