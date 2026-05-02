package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.usecase.csv.CsvTransactionExporter
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.util.logging.Logger

data class ExportTransactionsToCsvCommand(
    val userId: UserId,
    val transactions: List<Transaction>
) : Command<String>

@Port(Side.APPLICATION)
interface ExportTransactionsToCsvUseCase : CommandHandler<ExportTransactionsToCsvCommand, String> {
    override val commandClass get() = ExportTransactionsToCsvCommand::class
}

@DomainService
class ExportTransactionsToCsvService : ExportTransactionsToCsvUseCase {

    companion object {
        private val logger = Logger.getLogger(ExportTransactionsToCsvService::class.java.name)
    }

    private val csvExporter = CsvTransactionExporter()

    override fun handle(command: ExportTransactionsToCsvCommand): Result<String> {
        return try {
            val csvContent = csvExporter.exportToCsv(command.transactions)
            logger.info("CSV export completed: ${command.transactions.filter { !it.isPreview }.size} transactions exported")
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
