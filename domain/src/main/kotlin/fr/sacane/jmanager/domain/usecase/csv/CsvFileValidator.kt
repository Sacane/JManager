package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.csv.*
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.utils.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.logging.Logger

/**
 * Validator for CSV file analysis before import
 *
 * Detects structural issues, format problems, and potential data quality issues
 * such as swapped columns or invalid data
 */
class CsvFileValidator {

    companion object {
        private val logger = Logger.getLogger(CsvFileValidator::class.java.name)
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        private const val EXPECTED_COLUMN_COUNT = 5
        private val EXPECTED_HEADERS = listOf("date", "label", "depense", "recette", "tag")

        private const val DATE_COLUMN = 0
        private const val LABEL_COLUMN = 1
        private const val DEPENSE_COLUMN = 2
        private const val RECETTE_COLUMN = 3
        private const val TAG_COLUMN = 4
    }

    /**
     * Validates a CSV file content and returns Result with validation report or error
     *
     * @param rows List of CSV rows (including header)
     * @param availableTags Available tags for the user
     * @return Result<CsvValidationReport> - Success with warnings or Failure with first critical error
     */
    fun validate(rows: List<Array<String>>, availableTags: List<Tag>): Result<CsvValidationReport> {
        val warnings = mutableListOf<CsvValidationIssue>()
        val suggestions = mutableListOf<String>()

        if (rows.isEmpty()) {
            return failure(ResultState.CSV_EMPTY_FILE, "CSV file is empty")
        }

        val headerValidation = validateHeader(rows[0])
        if (headerValidation.isFailure()) {
            return headerValidation.map { CsvValidationReport(0, 0) }
        }

        val dataRows = rows.drop(1)
        var validLineCount = 0

        for ((index, row) in dataRows.withIndex()) {
            val lineNumber = index + 2
            val lineValidation = validateDataLine(row, lineNumber, availableTags)

            if (lineValidation.isFailure()) {
                return lineValidation.map { CsvValidationReport(dataRows.size, 0) }
            }

            lineValidation.mapNullable { lineWarnings ->
                lineWarnings?.let { warnings.addAll(it) }
            }
            validLineCount++
        }

        detectColumnSwapPatterns(dataRows, suggestions)

        return success(CsvValidationReport(
            totalLines = dataRows.size,
            validLines = validLineCount,
            warnings = warnings,
            suggestions = suggestions
        ))
    }

    private fun validateHeader(header: Array<String>): Result<Unit> {
        if (header.size < EXPECTED_COLUMN_COUNT) {
            return failure(
                ResultState.CSV_MISSING_COLUMNS,
                "Expected $EXPECTED_COLUMN_COUNT columns but found ${header.size}. Expected: ${EXPECTED_HEADERS.joinToString(", ")}"
            )
        }

        if (header.size > EXPECTED_COLUMN_COUNT) {
            return failure(
                ResultState.CSV_EXTRA_COLUMNS,
                "Expected $EXPECTED_COLUMN_COUNT columns but found ${header.size}. Expected: ${EXPECTED_HEADERS.joinToString(", ")}"
            )
        }

        val normalizedHeader = header.map { it.trim().lowercase() }
        if (normalizedHeader != EXPECTED_HEADERS) {
            val missingHeaders = EXPECTED_HEADERS.filterNot { it in normalizedHeader }
            val unexpectedHeaders = normalizedHeader.filterNot { it in EXPECTED_HEADERS }

            val errorMessage = buildString {
                append("Invalid header format. Expected: ${EXPECTED_HEADERS.joinToString(", ")}")
                if (missingHeaders.isNotEmpty()) {
                    append(". Missing: ${missingHeaders.joinToString(", ")}")
                }
                if (unexpectedHeaders.isNotEmpty()) {
                    append(". Unexpected: ${unexpectedHeaders.joinToString(", ")}")
                }
            }

            return failure(ResultState.CSV_INVALID_HEADER, errorMessage)
        }

        return success(Unit)
    }

    private fun validateDataLine(
        row: Array<String>,
        lineNumber: Int,
        availableTags: List<Tag>
    ): Result<List<CsvValidationIssue>> {
        val warnings = mutableListOf<CsvValidationIssue>()

        if (row.size != EXPECTED_COLUMN_COUNT) {
            return failure(
                ResultState.CSV_MALFORMED_LINE,
                "Line $lineNumber has ${row.size} columns instead of $EXPECTED_COLUMN_COUNT"
            )
        }

        val dateValidation = validateDateColumn(row[DATE_COLUMN], lineNumber)
        if (dateValidation.isFailure()) return dateValidation.map { emptyList() }

        val labelValidation = validateLabelColumn(row[LABEL_COLUMN], lineNumber)
        if (labelValidation.isFailure()) return labelValidation.map { emptyList() }

        val amountValidation = validateAmountColumns(row[DEPENSE_COLUMN], row[RECETTE_COLUMN], lineNumber)
        if (amountValidation.isFailure()) return amountValidation.map { emptyList() }

        amountValidation.mapNullable { amountWarnings ->
            amountWarnings?.let { warnings.addAll(it) }
        }

        val tagWarning = validateTagColumn(row[TAG_COLUMN], lineNumber, availableTags)
        tagWarning?.let { warnings.add(it) }

        return success(warnings)
    }

    private fun validateDateColumn(dateStr: String, lineNumber: Int): Result<Unit> {
        if (dateStr.isBlank()) {
            return failure(ResultState.CSV_MISSING_REQUIRED_FIELD, "Line $lineNumber: Date is required")
        }

        if (looksLikeAmount(dateStr)) {
            return failure(
                ResultState.CSV_POSSIBLE_COLUMN_SWAP,
                "Line $lineNumber: Date column contains what looks like an amount ($dateStr). Check for column swap"
            )
        }

        try {
            LocalDate.parse(dateStr.trim(), DATE_FORMATTER)
        } catch (e: DateTimeParseException) {
            return failure(
                ResultState.CSV_INVALID_DATE_FORMAT,
                "Line $lineNumber: Invalid date format '$dateStr'. Expected: dd-MM-yyyy (e.g., 15-01-2025)"
            )
        }

        return success(Unit)
    }

    private fun validateLabelColumn(labelStr: String, lineNumber: Int): Result<Unit> {
        if (labelStr.isBlank()) {
            return failure(ResultState.CSV_MISSING_REQUIRED_FIELD, "Line $lineNumber: Label is required")
        }

        if (looksLikeAmount(labelStr)) {
            return failure(
                ResultState.CSV_POSSIBLE_COLUMN_SWAP,
                "Line $lineNumber: Label looks like a numeric value ($labelStr). Check if columns are swapped"
            )
        }

        if (labelStr.trim().length > 200) {
            return failure(
                ResultState.CSV_MISSING_REQUIRED_FIELD,
                "Line $lineNumber: Label exceeds maximum length of 200 characters"
            )
        }

        return success(Unit)
    }

    private fun validateAmountColumns(
        depenseStr: String,
        recetteStr: String,
        lineNumber: Int
    ): Result<List<CsvValidationIssue>> {
        val warnings = mutableListOf<CsvValidationIssue>()
        val depenseEmpty = depenseStr.isBlank()
        val recetteEmpty = recetteStr.isBlank()

        if (depenseEmpty && recetteEmpty) {
            return failure(
                ResultState.CSV_NO_AMOUNT_FILLED,
                "Line $lineNumber: Either 'depense' or 'recette' must be filled"
            )
        }

        if (!depenseEmpty && !recetteEmpty) {
            return failure(
                ResultState.CSV_BOTH_AMOUNTS_FILLED,
                "Line $lineNumber: Only one of 'depense' or 'recette' should be filled, not both (depense: $depenseStr, recette: $recetteStr)"
            )
        }

        val amountStr = if (!depenseEmpty) depenseStr else recetteStr
        val fieldName = if (!depenseEmpty) "depense" else "recette"

        if (looksLikeText(amountStr)) {
            warnings.add(CsvValidationIssue(
                lineNumber = lineNumber,
                message = "Amount column ($fieldName) contains what looks like text: '$amountStr'. Check for column swap",
                detectedValue = amountStr
            ))
        }

        try {
            val normalizedStr = amountStr.trim().replace(',', '.')
            val value = BigDecimal(normalizedStr)

            if (value < BigDecimal.ZERO) {
                return failure(
                    ResultState.CSV_NEGATIVE_AMOUNT,
                    "Line $lineNumber: Amount cannot be negative ($amountStr)"
                )
            }
        } catch (e: NumberFormatException) {
            return failure(
                ResultState.CSV_INVALID_AMOUNT_FORMAT,
                "Line $lineNumber: Invalid amount format for $fieldName: '$amountStr'. Use numbers with dot or comma (e.g., 123.45 or 123,45)"
            )
        }

        return success(warnings)
    }

    private fun validateTagColumn(
        tagStr: String,
        lineNumber: Int,
        availableTags: List<Tag>
    ): CsvValidationIssue? {
        if (tagStr.isBlank()) {
            return null
        }

        val trimmedTag = tagStr.trim()
        val allTags = availableTags + defaultTags

        val tagExists = allTags.any { it.label.equals(trimmedTag, ignoreCase = true) }

        if (!tagExists) {
            return CsvValidationIssue(
                lineNumber = lineNumber,
                message = "Tag '$trimmedTag' not found. Will be replaced with 'Aucune'",
                detectedValue = tagStr
            )
        }

        return null
    }

    private fun detectColumnSwapPatterns(rows: List<Array<String>>, suggestions: MutableList<String>) {
        if (rows.isEmpty()) return

        var labelLooksLikeAmountCount = 0
        var amountLooksLikeTextCount = 0

        rows.take(10).forEach { row ->
            if (row.size >= EXPECTED_COLUMN_COUNT) {
                if (looksLikeAmount(row[LABEL_COLUMN])) {
                    labelLooksLikeAmountCount++
                }

                val depenseStr = row[DEPENSE_COLUMN]
                val recetteStr = row[RECETTE_COLUMN]
                if (looksLikeText(depenseStr) || looksLikeText(recetteStr)) {
                    amountLooksLikeTextCount++
                }
            }
        }

        if (labelLooksLikeAmountCount >= 3 || amountLooksLikeTextCount >= 3) {
            suggestions.add("⚠️ Possible column swap detected: labels and amounts might be swapped. Please verify your CSV structure.")
        }
    }

    private fun looksLikeAmount(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return false

        val normalized = trimmed.replace(',', '.')
        return try {
            BigDecimal(normalized)
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun looksLikeText(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return false

        val hasLetters = trimmed.any { it.isLetter() }
        val hasMultipleWords = trimmed.contains(' ')

        return hasLetters && (hasMultipleWords || trimmed.length > 5)
    }
}

