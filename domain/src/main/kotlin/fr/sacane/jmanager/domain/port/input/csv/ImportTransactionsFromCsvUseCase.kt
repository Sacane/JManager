package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.csv.CsvImportResult
import fr.sacane.jmanager.domain.port.output.CsvFileReader
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.csv.CsvFileValidator
import fr.sacane.jmanager.domain.usecase.csv.CsvTransactionValidator
import fr.sacane.jmanager.domain.usecase.csv.CsvValidationUtils
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID
import org.slf4j.LoggerFactory

data class ImportTransactionsFromCsvCommand(
    val userId: UserId,
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
    private val unitOfWorkProvider: UnitOfWorkTransactionProvider
) : ImportTransactionsFromCsvUseCase {

    companion object {
        private val log = LoggerFactory.getLogger(ImportTransactionsFromCsvService::class.java)
    }

    private val validator = CsvTransactionValidator()
    private val fileValidator = CsvFileValidator()

    override fun handle(command: ImportTransactionsFromCsvCommand): Result<CsvImportResult> {
        val userId = command.userId
        val bookletFindResult = findBookletAndCheckOwner(bookletRepository, userId, command.bookletId)

        val userTags = tagRepository.getAllDefault(userId)
        val csvSeparator = CsvValidationUtils.detectCsvSeparator(command.csvContent)

        try {
            val rows = csvFileReader.readCsvContent(command.csvContent)

            if (!command.skipValidation) {
                val validationResult = bookletFindResult.flatMap { checkValidationErrors(fileValidator, rows, userTags, command.month, command.year, csvSeparator) }
                if (validationResult.isFailure()) return validationResult
            }

            return bookletFindResult.flatMap { booklet ->
                    unitOfWorkProvider.executeInTransaction(booklet) { bookletParam ->
                        val results = convertRowsToTransactions(validator, rows, userTags, command.skipValidation, command.month, command.year)
                        val csvImportResult = saveTransactions(transactionRepository, bookletRepository, bookletParam, results)

                        log.info("CSV import completed: {} transactions imported, {} failures", csvImportResult.successCount, csvImportResult.failedLines.size)
                    success(csvImportResult)
                }
            }
        } catch (e: Exception) {
            log.error("CSV import failed unexpectedly", e)
            return csvDomainFailure(
                ResultState.INTERNAL_SERVER_ERROR,
                "Error during import: ${e.message}",
                "domain.file.import.internal_error"
            )
        }
    }
}
