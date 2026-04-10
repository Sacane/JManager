package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.time.format.DateTimeFormatter

/**
 * Use case for exporting transactions to CSV format
 *
 * Generates CSV content with the following columns:
 * - date (format dd-MM-yyyy)
 * - label
 * - depense (expense - only for non-income transactions)
 * - recette (income - only for income transactions)
 * - tag (tag name or empty)
 */
@UseCase
class CsvTransactionExporter {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        private const val CSV_SEPARATOR = ";"
        private const val CSV_HEADER = "date;label;depense;recette;tag"
    }

    /**
     * Exports transactions to CSV format
     * Filters out preview transactions (isPreview = true) automatically
     *
     * @param transactions List of transactions to export
     * @return CSV content as String with header and transaction rows
     */
    fun exportToCsv(transactions: List<Transaction>): String {
        val nonPreviewTransactions = transactions.filter { !it.isPreview }

        if (nonPreviewTransactions.isEmpty()) {
            return CSV_HEADER
        }

        val csvRows = mutableListOf(CSV_HEADER)

        nonPreviewTransactions.forEach { transaction ->
            csvRows.add(transactionToCsvRow(transaction))
        }

        return csvRows.joinToString("\n")
    }

    private fun transactionToCsvRow(transaction: Transaction): String {
        val date = transaction.date.format(DATE_FORMATTER)
        val label = escapeCsvField(transaction.label)
        val depense = if (!transaction.isIncome) formatAmount(transaction.amount.value) else ""
        val recette = if (transaction.isIncome) formatAmount(transaction.amount.value) else ""
        val tag = transaction.tag?.label ?: ""

        return listOf(date, label, depense, recette, tag).joinToString(CSV_SEPARATOR)
    }


    private fun formatAmount(value: java.math.BigDecimal): String {
        return value.toPlainString().replace('.', ',')
    }


    private fun escapeCsvField(field: String): String {
        var sanitized = field
        // Protect against CSV formula injection (=, +, -, @, \t, \r can trigger formulas in spreadsheet apps)
        if (sanitized.isNotEmpty() && sanitized[0] in charArrayOf('=', '+', '-', '@', '\t', '\r')) {
            sanitized = "'$sanitized"
        }
        if (sanitized.contains(CSV_SEPARATOR) || sanitized.contains("\"") || sanitized.contains("\n")) {
            return "\"${sanitized.replace("\"", "\"\"")}\""
        }
        return sanitized
    }
}

