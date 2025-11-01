package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.csv.CsvReportType
import fr.sacane.jmanager.domain.models.csv.CsvValidationIssue
import fr.sacane.jmanager.domain.models.csv.CsvValidationReport
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success
import java.math.BigDecimal

/**
 * Validator for CSV file analysis before import
 *
 * Detects structural issues, format problems, and potential data quality issues
 * such as swapped columns or invalid data
 */
class CsvFileValidator {

    companion object {
        private const val EXPECTED_COLUMN_COUNT = 5
        private val EXPECTED_HEADERS = listOf("date", "label", "depense", "recette", "tag")

        private const val DATE_COLUMN = 0
        private const val LABEL_COLUMN = 1
        private const val DEPENSE_COLUMN = 2
        private const val RECETTE_COLUMN = 3
        private const val TAG_COLUMN = 4

        // Security limits
        private const val MAX_ROWS = 10000
        private const val MAX_LINE_LENGTH = 5000
    }

    /**
     * Validates a CSV file content and returns Result with validation report
     * Always returns success - errors are included in the report, not as Result failure
     *
     * @param rows List of CSV rows (including header)
     * @param availableTags Available tags for the user
     * @return Result<CsvValidationReport> - Always success with errors/warnings in the report
     */
    fun validate(rows: List<Array<String>>, availableTags: List<Tag>): Result<CsvValidationReport> {
        val errors = mutableListOf<CsvValidationIssue>()
        val warnings = mutableListOf<CsvValidationIssue>()
        val suggestions = mutableListOf<String>()

        if (rows.isEmpty()) {
            errors.add(CsvValidationIssue(
                lineNumber = 0,
                type = CsvReportType.EMPTY_FILE,
                message = "CSV file is empty"
            ))
            return success(CsvValidationReport(0, 0, errors, warnings, suggestions))
        }

        if (rows.size > MAX_ROWS + 1) {
            errors.add(CsvValidationIssue(
                lineNumber = 0,
                type = CsvReportType.TOO_MANY_ROWS,
                message = "Le fichier contient trop de lignes (${rows.size - 1}). Maximum autorisé: $MAX_ROWS transactions"
            ))
            return success(CsvValidationReport(0, 0, errors, warnings, suggestions))
        }

        val headerValidation = validateHeader(rows[0])
        if (headerValidation != null) {
            errors.add(headerValidation)
            return success(CsvValidationReport(0, 0, errors, warnings, suggestions))
        }

        val dataRows = rows.drop(1)
        var validLineCount = 0

        for ((index, row) in dataRows.withIndex()) {
            val lineNumber = index + 2
            val lineValidation = validateDataLine(row, lineNumber, availableTags)

            if (lineValidation.error != null) {
                errors.add(lineValidation.error)
            } else {
                lineValidation.warnings?.let { warnings.addAll(it) }
                validLineCount++
            }
        }

        detectColumnSwapPatterns(dataRows, suggestions)

        return success(CsvValidationReport(
            totalLines = dataRows.size,
            validLines = validLineCount,
            errors = errors,
            warnings = warnings,
            suggestions = suggestions
        ))
    }

    private fun validateHeader(header: Array<String>): CsvValidationIssue? {
        if (header.size < EXPECTED_COLUMN_COUNT) {
            return CsvValidationIssue(
                lineNumber = 1,
                type = CsvReportType.MISSING_COLUMNS,
                message = "Expected $EXPECTED_COLUMN_COUNT columns but found ${header.size}. Expected: ${EXPECTED_HEADERS.joinToString(", ")}"
            )
        }

        if (header.size > EXPECTED_COLUMN_COUNT) {
            return CsvValidationIssue(
                lineNumber = 1,
                type = CsvReportType.EXTRA_COLUMNS,
                message = "Expected $EXPECTED_COLUMN_COUNT columns but found ${header.size}. Expected: ${EXPECTED_HEADERS.joinToString(", ")}"
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

            return CsvValidationIssue(
                lineNumber = 1,
                type = CsvReportType.INVALID_HEADER,
                message = errorMessage
            )
        }

        return null
    }

    private data class LineValidationResult(
        val error: CsvValidationIssue? = null,
        val warnings: List<CsvValidationIssue>? = null
    )

    private fun validateDataLine(
        row: Array<String>,
        lineNumber: Int,
        availableTags: List<Tag>
    ): LineValidationResult {
        val warnings = mutableListOf<CsvValidationIssue>()

        if (row.size != EXPECTED_COLUMN_COUNT) {
            return LineValidationResult(
                error = CsvValidationIssue(
                    lineNumber = lineNumber,
                    type = CsvReportType.MALFORMED_LINE,
                    message = "Line $lineNumber has ${row.size} columns instead of $EXPECTED_COLUMN_COUNT"
                )
            )
        }

        val dateError = validateDateColumn(row[DATE_COLUMN], lineNumber)
        if (dateError != null) return LineValidationResult(error = dateError)

        val labelError = validateLabelColumn(row[LABEL_COLUMN], lineNumber)
        if (labelError != null) return LineValidationResult(error = labelError)

        val amountResult = validateAmountColumns(row[DEPENSE_COLUMN], row[RECETTE_COLUMN], lineNumber)
        if (amountResult.error != null) return LineValidationResult(error = amountResult.error)
        amountResult.warnings?.let { warnings.addAll(it) }

        val tagWarning = validateTagColumn(row[TAG_COLUMN], lineNumber, availableTags)
        tagWarning?.let { warnings.add(it) }

        return LineValidationResult(warnings = warnings)
    }

    private fun validateDateColumn(dateStr: String, lineNumber: Int): CsvValidationIssue? {
        if (dateStr.isBlank()) {
            return CsvValidationIssue(
                lineNumber = lineNumber,
                type = CsvReportType.MISSING_REQUIRED_FIELD,
                message = "Line $lineNumber: Date is required"
            )
        }

        if (CsvValidationUtils.looksLikeAmount(dateStr)) {
            return CsvValidationIssue(
                lineNumber = lineNumber,
                type = CsvReportType.POSSIBLE_COLUMN_SWAP,
                message = "Line $lineNumber: Date column contains what looks like an amount ($dateStr). Check for column swap"
            )
        }

        val date = CsvValidationUtils.parseDate(dateStr)
        if (date == null) {
            return CsvValidationIssue(
                lineNumber = lineNumber,
                type = CsvReportType.INVALID_DATE_FORMAT,
                message = "Line $lineNumber: Invalid date format '$dateStr'. Expected: dd-MM-yyyy (e.g., 15-01-2025)"
            )
        }

        return null
    }

    private fun validateLabelColumn(labelStr: String, lineNumber: Int): CsvValidationIssue? {
        if (labelStr.isBlank()) {
            return CsvValidationIssue(
                lineNumber = lineNumber,
                type = CsvReportType.MISSING_REQUIRED_FIELD,
                message = "Line $lineNumber: Label is required"
            )
        }

        if (CsvValidationUtils.looksLikeAmount(labelStr)) {
            return CsvValidationIssue(
                lineNumber = lineNumber,
                type = CsvReportType.POSSIBLE_COLUMN_SWAP,
                message = "Line $lineNumber: Label looks like a numeric value ($labelStr). Check if columns are swapped"
            )
        }

        if (labelStr.trim().length > 200) {
            return CsvValidationIssue(
                lineNumber = lineNumber,
                type = CsvReportType.MISSING_REQUIRED_FIELD,
                message = "Line $lineNumber: Label exceeds maximum length of 200 characters"
            )
        }

        return null
    }

    private fun validateAmountColumns(
        depenseStr: String,
        recetteStr: String,
        lineNumber: Int
    ): LineValidationResult {
        val warnings = mutableListOf<CsvValidationIssue>()
        val depenseEmpty = depenseStr.isBlank()
        val recetteEmpty = recetteStr.isBlank()

        if (depenseEmpty && recetteEmpty) {
            return LineValidationResult(
                error = CsvValidationIssue(
                    lineNumber = lineNumber,
                    type = CsvReportType.NO_AMOUNT_FILLED,
                    message = "Line $lineNumber: Either 'depense' or 'recette' must be filled"
                )
            )
        }

        if (!depenseEmpty && !recetteEmpty) {
            return LineValidationResult(
                error = CsvValidationIssue(
                    lineNumber = lineNumber,
                    type = CsvReportType.BOTH_AMOUNTS_FILLED,
                    message = "Line $lineNumber: Only one of 'depense' or 'recette' should be filled, not both (depense: $depenseStr, recette: $recetteStr)"
                )
            )
        }

        val amountStr = if (!depenseEmpty) depenseStr else recetteStr
        val fieldName = if (!depenseEmpty) "depense" else "recette"

        if (CsvValidationUtils.looksLikeText(amountStr)) {
            warnings.add(CsvValidationIssue(
                lineNumber = lineNumber,
                type = CsvReportType.INVALID_AMOUNT_FORMAT,
                message = "Amount column ($fieldName) contains what looks like text: '$amountStr'. Check for column swap",
                detectedValue = amountStr
            ))
        }

        val value = CsvValidationUtils.parseAmountValue(amountStr.trim())
        if (value == null) {
            return LineValidationResult(
                error = CsvValidationIssue(
                    lineNumber = lineNumber,
                    type = CsvReportType.INVALID_AMOUNT_FORMAT,
                    message = "Line $lineNumber: Invalid amount format for $fieldName: '$amountStr'. Use numbers with dot or comma (e.g., 123.45 or 123,45)"
                )
            )
        }

        if (value < BigDecimal.ZERO) {
            return LineValidationResult(
                error = CsvValidationIssue(
                    lineNumber = lineNumber,
                    type = CsvReportType.NEGATIVE_AMOUNT,
                    message = "Line $lineNumber: Amount cannot be negative ($amountStr)"
                )
            )
        }

        return LineValidationResult(warnings = warnings)
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
                type = CsvReportType.UNKNOWN_TAG,
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
                if (CsvValidationUtils.looksLikeAmount(row[LABEL_COLUMN])) {
                    labelLooksLikeAmountCount++
                }

                val depenseStr = row[DEPENSE_COLUMN]
                val recetteStr = row[RECETTE_COLUMN]
                if (CsvValidationUtils.looksLikeText(depenseStr) || CsvValidationUtils.looksLikeText(recetteStr)) {
                    amountLooksLikeTextCount++
                }
            }
        }

        if (labelLooksLikeAmountCount >= 3 || amountLooksLikeTextCount >= 3) {
            suggestions.add("⚠️ Possible column swap detected: labels and amounts might be swapped. Please verify your CSV structure.")
        }
    }
}

