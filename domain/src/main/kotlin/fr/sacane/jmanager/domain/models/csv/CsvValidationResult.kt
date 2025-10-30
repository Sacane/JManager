package fr.sacane.jmanager.domain.models.csv

/**
 * Represents an issue detected during CSV validation
 *
 * @property lineNumber The line number where the issue was found
 * @property message Human-readable description of the issue
 * @property detectedValue The value that caused the issue, if applicable
 */
data class CsvValidationIssue(
    val lineNumber: Int,
    val message: String,
    val detectedValue: String? = null
)

/**
 * Validation report containing warnings and suggestions
 * This is used as the data payload in Result<CsvValidationReport>
 *
 * @property totalLines Total number of data lines in the CSV (excluding header)
 * @property validLines Number of valid lines
 * @property warnings List of warning issues (non-blocking)
 * @property suggestions List of suggestions to improve the CSV
 */
data class CsvValidationReport(
    val totalLines: Int,
    val validLines: Int,
    val warnings: List<CsvValidationIssue> = emptyList(),
    val suggestions: List<String> = emptyList()
)

