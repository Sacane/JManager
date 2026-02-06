package fr.sacane.jmanager.domain.models.csv


enum class CsvReportType {
    EMPTY_FILE,
    TOO_MANY_ROWS,
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
    UNKNOWN_TAG,
    DECIMAL_SEPARATOR_INCONSISTENT,
    DECIMAL_SEPARATOR_EQUALS_CSV_SEPARATOR
}

data class CsvValidationIssue(
    val lineNumber: Int,
    val type: CsvReportType,
    val message: String,
    val detectedValue: String? = null
)

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

