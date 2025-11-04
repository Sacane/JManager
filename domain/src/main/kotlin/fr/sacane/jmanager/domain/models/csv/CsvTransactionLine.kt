package fr.sacane.jmanager.domain.models.csv

import fr.sacane.jmanager.domain.models.transaction.Transaction

data class CsvTransactionLine(
    val lineNumber: Int,
    val date: String,
    val label: String,
    val depense: String,
    val recette: String,
    val tag: String
)

sealed class CsvLineResult {
    data class Success(val transaction: Transaction) : CsvLineResult()
    data class Error(val lineNumber: Int, val errors: List<String>) : CsvLineResult()
}

data class CsvImportResult(
    val successCount: Int,
    val failedLines: List<CsvLineResult.Error>,
    val transactions: List<Transaction>
) {
    val hasErrors: Boolean get() = failedLines.isNotEmpty()
    val totalProcessed: Int get() = successCount + failedLines.size
}

