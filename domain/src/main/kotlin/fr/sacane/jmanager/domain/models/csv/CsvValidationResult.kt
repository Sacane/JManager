package fr.sacane.jmanager.domain.models.csv

/**
 * Type of CSV validation issue
 */
enum class CsvReportType {
    EMPTY_FILE,
    INVALID_HEADER,
    MISSING_COLUMNS,
    EXTRA_COLUMNS,
    MALFORMED_LINE,
    INVALID_DATE_FORMAT,
    INVALID_AMOUNT_FORMAT,
    MISSING_REQUIRED_FIELD,
    BOTH_AMOUNTS_FILLED,
    NO_AMOUNT_FILLED,
    NEGATIVE_AMOUNT,
    POSSIBLE_COLUMN_SWAP,
    UNKNOWN_TAG
}

/**
 * Represents an issue detected during CSV validation
 *
 * @property lineNumber The line number where the issue was found
 * @property type The type of validation issue
 * @property message Human-readable description of the issue
 * @property detectedValue The value that caused the issue, if applicable
 */
data class CsvValidationIssue(
    val lineNumber: Int,
    val type: CsvReportType,
    val message: String,
    val detectedValue: String? = null
)

/**
 * Validation report containing errors, warnings and suggestions
 * Always returned with HTTP 200 - the analysis was performed successfully
 *
 * @property totalLines Total number of data lines in the CSV (excluding header)
 * @property validLines Number of valid lines
 * @property errors List of critical errors that prevent import
 * @property warnings List of warning issues (non-blocking)
 * @property suggestions List of suggestions to improve the CSV
 */
data class CsvValidationReport(
    val totalLines: Int,
    val validLines: Int,
    val errors: List<CsvValidationIssue> = emptyList(),
    val warnings: List<CsvValidationIssue> = emptyList(),
    val suggestions: List<String> = emptyList()
) {
    val hasErrors: Boolean get() = errors.isNotEmpty()
    val canImport: Boolean get() = !hasErrors
}

