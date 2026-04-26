package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.usecase.csv.CsvTransactionExporter
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.util.logging.Logger

@DomainService
class ExportTransactionsToCsvService(
    private val sessionManager: SessionManager
) : ExportTransactionsToCsvUseCase {

    companion object {
        private val logger = Logger.getLogger(ExportTransactionsToCsvService::class.java.name)
    }

    private val csvExporter = CsvTransactionExporter()

    override fun exportTransactionsToCsv(
        token: SessionToken,
        transactions: List<Transaction>
    ): Result<String> {
        return sessionManager.authenticate(token) { _ ->
            try {
                val csvContent = csvExporter.exportToCsv(transactions)
                logger.info("CSV export completed: ${transactions.filter { !it.isPreview }.size} transactions exported")
                success(csvContent)
            } catch (e: Exception) {
                logger.severe("Error during CSV export: ${e.message}")
                csvDomainFailure(
                    ResultState.INTERNAL_SERVER_ERROR,
                    "Error during export: ${e.message}",
                    "domain.file.export.internal_error"
                )
            }
        }
    }
}
