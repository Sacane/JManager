package fr.sacane.jmanager.infrastructure.api.csv

import fr.sacane.jmanager.domain.models.csv.CsvValidationReport
import fr.sacane.jmanager.domain.models.csv.CsvValidationIssue
import fr.sacane.jmanager.domain.models.csv.CsvImportResult
import fr.sacane.jmanager.domain.models.csv.CsvLineResult
import kotlinx.serialization.Serializable

/**
 * DTO for CSV validation report
 */
@Serializable
data class CsvValidationReportDTO(
    val totalLines: Int,
    val validLines: Int,
    val errors: List<CsvValidationIssueDTO>,
    val warnings: List<CsvValidationIssueDTO>,
    val suggestions: List<String>,
    val hasErrors: Boolean,
    val canImport: Boolean
)

/**
 * DTO for CSV validation issue (error or warning)
 */
@Serializable
data class CsvValidationIssueDTO(
    val lineNumber: Int,
    val type: String,
    val message: String,
    val detectedValue: String?
)

/**
 * DTO for CSV import result
 */
@Serializable
data class CsvImportResultDTO(
    val successCount: Int,
    val failedCount: Int,
    val totalProcessed: Int,
    val hasErrors: Boolean,
    val transactions: List<TransactionDTO>,
    val errors: List<CsvLineErrorDTO>
)

/**
 * DTO for CSV line error
 */
@Serializable
data class CsvLineErrorDTO(
    val lineNumber: Int,
    val errors: List<String>
)

/**
 * DTO for transaction in CSV import result
 */
@Serializable
data class TransactionDTO(
    val id: String?,
    val label: String,
    val date: String,
    val amount: String,
    val isIncome: Boolean,
    val tag: String?
)

fun CsvValidationReport.toDTO(): CsvValidationReportDTO {
    return CsvValidationReportDTO(
        totalLines = this.totalLines,
        validLines = this.validLines,
        errors = this.errors.map { it.toDTO() },
        warnings = this.warnings.map { it.toDTO() },
        suggestions = this.suggestions,
        hasErrors = this.hasErrors,
        canImport = this.canImport
    )
}

fun CsvValidationIssue.toDTO(): CsvValidationIssueDTO {
    return CsvValidationIssueDTO(
        lineNumber = this.lineNumber,
        type = this.type.name,
        message = this.message,
        detectedValue = this.detectedValue
    )
}

fun CsvImportResult.toDTO(): CsvImportResultDTO {
    return CsvImportResultDTO(
        successCount = this.successCount,
        failedCount = this.failedLines.size,
        totalProcessed = this.totalProcessed,
        hasErrors = this.hasErrors,
        transactions = this.transactions.map { transaction ->
            TransactionDTO(
                id = transaction.id?.toString(),
                label = transaction.label,
                date = transaction.date.toString(),
                amount = transaction.amount.value.toString(),
                isIncome = transaction.isIncome,
                tag = transaction.tag?.label
            )
        },
        errors = this.failedLines.map { it.toDTO() }
    )
}

fun CsvLineResult.Error.toDTO(): CsvLineErrorDTO {
    return CsvLineErrorDTO(
        lineNumber = this.lineNumber,
        errors = this.errors
    )
}


