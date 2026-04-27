package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.csv.CsvImportResult
import fr.sacane.jmanager.domain.port.spi.CsvFileReader
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.csv.CsvFileValidator
import fr.sacane.jmanager.domain.usecase.csv.CsvTransactionValidator
import fr.sacane.jmanager.domain.usecase.csv.CsvValidationUtils
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID
import java.util.logging.Logger

data class ImportTransactionsFromCsvCommand(
    val token: SessionToken,
    val bookletId: UUID,
    val csvContent: String,
    val skipValidation: Boolean = false,
    val month: Int? = null,
    val year: Int? = null
) : Command<CsvImportResult>

@Port(Side.APPLICATION)
interface ImportTransactionsFromCsvUseCase : CommandHandler<ImportTransactionsFromCsvCommand, CsvImportResult> {
    override val commandClass get() = ImportTransactionsFromCsvCommand::class
}

@DomainService
class ImportTransactionsFromCsvService(
    private val csvFileReader: CsvFileReader,
    private val transactionRepository: TransactionRepository,
    private val bookletRepository: BookletRepository,
    private val tagRepository: TagRepository,
    private val sessionManager: SessionManager,
    private val unitOfWorkProvider: UnitOfWorkTransactionProvider
) : ImportTransactionsFromCsvUseCase {

    companion object {
        private val logger = Logger.getLogger(ImportTransactionsFromCsvService::class.java.name)
    }

    private val validator = CsvTransactionValidator()
    private val fileValidator = CsvFileValidator()

    override fun handle(command: ImportTransactionsFromCsvCommand): Result<CsvImportResult> {
        return sessionManager.authenticate(command.token) { userId ->
            val bookletFindResult = findBookletAndCheckOwner(bookletRepository, userId, command.bookletId)

            val userTags = tagRepository.getAllDefault(userId)
            val csvSeparator = CsvValidationUtils.detectCsvSeparator(command.csvContent)

            try {
                val rows = csvFileReader.readCsvContent(command.csvContent)

                if (!command.skipValidation) {
                    val validationResult = bookletFindResult.flatMap { checkValidationErrors(fileValidator, rows, userTags, command.month, command.year, csvSeparator) }
                    if (validationResult.isFailure()) return@authenticate validationResult
                }

                bookletFindResult.flatMap { booklet ->
                    unitOfWorkProvider.executeInTransaction(booklet) { bookletParam ->
                        val results = convertRowsToTransactions(validator, rows, userTags, command.skipValidation, command.month, command.year)
                        val csvImportResult = saveTransactions(transactionRepository, bookletRepository, bookletParam, results)

                        logger.info("CSV import completed: ${csvImportResult.successCount} transactions imported, ${csvImportResult.failedLines.size} errors")
                        success(csvImportResult)
                    }
                }
            } catch (e: Exception) {
                logger.severe("Error during CSV import: ${e.message}")
                csvDomainFailure(
                    ResultState.INTERNAL_SERVER_ERROR,
                    "Error during import: ${e.message}",
                    "domain.file.import.internal_error"
                )
            }
        }
    }
}
