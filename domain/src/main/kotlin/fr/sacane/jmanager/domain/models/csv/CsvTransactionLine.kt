package fr.sacane.jmanager.domain.models.csv

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Représente une ligne du fichier CSV d'import de transactions
 */
data class CsvTransactionLine(
    val lineNumber: Int,
    val date: String,
    val label: String,
    val depense: String,
    val recette: String,
    val tag: String
)

/**
 * Résultat de la validation et conversion d'une ligne CSV en Transaction
 */
sealed class CsvLineResult {
    data class Success(val transaction: Transaction) : CsvLineResult()
    data class Error(val lineNumber: Int, val errors: List<String>) : CsvLineResult()
}

/**
 * Résultat global de l'import CSV
 */
data class CsvImportResult(
    val successCount: Int,
    val failedLines: List<CsvLineResult.Error>,
    val transactions: List<Transaction>
) {
    val hasErrors: Boolean get() = failedLines.isNotEmpty()
    val totalProcessed: Int get() = successCount + failedLines.size
}

