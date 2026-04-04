package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.csv.CsvImportResult
import fr.sacane.jmanager.domain.models.csv.CsvTransactionLine
import fr.sacane.jmanager.domain.models.csv.CsvLineResult
import fr.sacane.jmanager.domain.models.csv.CsvValidationReport
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.CsvFileReader
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.csv.CsvTransactionValidator
import fr.sacane.jmanager.domain.usecase.csv.CsvFileValidator
import fr.sacane.jmanager.domain.usecase.csv.CsvTransactionExporter
import fr.sacane.jmanager.domain.utils.*
import java.util.*
import java.util.logging.Logger

@Port(Side.APPLICATION)
/**
 * Application port: FileImportExportFeature
 *
 * Allows importing and exporting transactions from/to files.
 * The file format (CSV, JSON, YAML, etc.) is an implementation detail handled by the infrastructure layer.
 *
 * Currently supports:
 * - CSV import with columns: date, label, depense (expense), recette (income), tag
 * - CSV export with the same format
 */
sealed interface FileImportExportFeature {

    /**
     * Validates CSV content before import
     *
     * @param token Authentication token
     * @param bookletId ID of the booklet to import transactions into
     * @param csvContent CSV file content (raw text)
     * @param month Optional month (1-12) to use when CSV date contains only day
     * @param year Optional year to use when CSV date contains only day
     * @return Result containing CsvValidationReport with warnings or error with first critical issue
     */
    fun validateCsvFile(
        token: String,
        bookletId: UUID,
        csvContent: String,
        month: Int? = null,
        year: Int? = null
    ): Result<CsvValidationReport>

    /**
     * Imports transactions from CSV content for a given booklet
     *
     * @param token Authentication token
     * @param bookletId ID of the booklet to import transactions into
     * @param csvContent CSV file content (raw text)
     * @param skipValidation If true, skips CSV validation (assumes it was already validated). Default: false for safety
     * @param month Optional month (1-12) to use when CSV date contains only day
     * @param year Optional year to use when CSV date contains only day
     * @return Result containing CsvImportResult with created transactions and potential errors
     */
    fun importTransactionsFromCsv(
        token: String,
        bookletId: UUID,
        csvContent: String,
        skipValidation: Boolean = false,
        month: Int? = null,
        year: Int? = null
    ): Result<CsvImportResult>

    /**
     * Exports non-preview transactions to CSV format
     *
     * @param token Authentication token
     * @param transactions List of transactions to export (only non-preview transactions will be exported)
     * @return Result containing CSV content as string with proper formatting
     */
    fun exportTransactionsToCsv(
        token: String,
        transactions: List<Transaction>
    ): Result<String>
}

@DomainService
class FileImportExportFeatureImpl(
    private val csvFileReader: CsvFileReader,
    private val transactionRepository: TransactionRepository,
    private val bookletRepository: BookletRepository,
    private val tagRepository: TagRepository,
    private val sessionManager: SessionManager,
    private val unitOfWorkProvider: UnitOfWorkTransactionProvider
) : FileImportExportFeature {

    companion object {
        private val logger = Logger.getLogger(FileImportExportFeatureImpl::class.java.name)

        private const val DATE_COLUMN = 0
        private const val LABEL_COLUMN = 1
        private const val DEPENSE_COLUMN = 2
        private const val RECETTE_COLUMN = 3
        private const val TAG_COLUMN = 4
        private const val EXPECTED_COLUMN_COUNT = 5
    }

    private val validator = CsvTransactionValidator()
    private val fileValidator = CsvFileValidator()
    private val csvExporter = CsvTransactionExporter()

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun validateCsvFile(
        token: String,
        bookletId: UUID,
        csvContent: String,
        month: Int?,
        year: Int?
    ): Result<CsvValidationReport> {
        return sessionManager.authenticate(token) { userId ->
            val bookletFindResult = findBookletAndCheckOwner(userId, bookletId)

            val userTags = tagRepository.getAllDefault(userId)
            val csvSeparator = fr.sacane.jmanager.domain.usecase.csv.CsvValidationUtils.detectCsvSeparator(csvContent)

            try {
                bookletFindResult.flatMap {
                    val rows = csvFileReader.readCsvContent(csvContent)
                    val validationResult = fileValidator.validate(rows, userTags, month, year, csvSeparator)

                    validationResult.mapNullable { report ->
                        if (report != null) {
                            logger.info("CSV validation completed: ${report.totalLines} lines, ${report.validLines} valid, ${report.warnings.size} warnings")
                        } else {
                            logger.warning("CSV validation failed: ${validationResult.message}")
                        }
                    }

                    validationResult
                }
            } catch (e: Exception) {
                logger.severe("Error during CSV validation: ${e.message}")
                domainFailure(
                    ResultState.INTERNAL_SERVER_ERROR,
                    "Error during validation: ${e.message}",
                    "domain.file.validation.internal_error"
                )
            }
        }
    }

    override fun importTransactionsFromCsv(
        token: String,
        bookletId: UUID,
        csvContent: String,
        skipValidation: Boolean,
        month: Int?,
        year: Int?
    ): Result<CsvImportResult> {
        return sessionManager.authenticate(token) { userId ->
            val bookletFindResult = findBookletAndCheckOwner(userId, bookletId)

            val userTags = tagRepository.getAllDefault(userId)
            val csvSeparator = fr.sacane.jmanager.domain.usecase.csv.CsvValidationUtils.detectCsvSeparator(csvContent)

            try {
                val rows = csvFileReader.readCsvContent(csvContent)

                if (!skipValidation) {
                    val validationResult = bookletFindResult.flatMap { checkValidationErrors(rows, userTags, month, year, csvSeparator) }
                    if (validationResult.isFailure()) return@authenticate validationResult
                }

                bookletFindResult.flatMap { processImport(it, rows, userTags, skipValidation, month, year) }
            } catch (e: Exception) {
                logger.severe("Error during CSV import: ${e.message}")
                domainFailure(
                    ResultState.INTERNAL_SERVER_ERROR,
                    "Error during import: ${e.message}",
                    "domain.file.import.internal_error"
                )
            }
        }
    }

    private fun findBookletAndCheckOwner(userId: UserId, bookletId: UUID): Result<Booklet> {
        val booklet =
            bookletRepository.findBookletByIdWithTransactions(bookletId)
                ?: return domainFailure(
                    ResultState.NOT_FOUND,
                    "Booklet not found",
                    "domain.file.booklet.not_found"
                )
        if (booklet.owner?.id?.value != userId.value) {
            return domainFailure(
                ResultState.FORBIDDEN,
                "You are not the owner of this booklet",
                "domain.file.booklet.forbidden_owner"
            )
        }
        return success(booklet)
    }

    private fun checkValidationErrors(rows: List<Array<String>>, userTags: List<Tag>, month: Int?, year: Int?, csvSeparator: Char?): Result<CsvImportResult> {
        val validationResult = fileValidator.validate(rows, userTags, month, year, csvSeparator)

        if (validationResult.isFailure()) {
            return domainFailure(
                validationResult.status,
                validationResult.message,
                validationResult.errorInfo?.key ?: "domain.file.import.validation_failed"
            )
        }

        val hasErrors = validationResult.mapNullable { report -> report?.hasErrors ?: false }
        if (hasErrors) {
            val errorMessages = validationResult.mapNullable { report ->
                report?.errors?.joinToString("; ") { "Line ${it.lineNumber}: ${it.message}" }
                    ?: "Unknown validation errors"
            }
            return domainFailure(
                ResultState.INVALID,
                "CSV validation failed: $errorMessages",
                "domain.file.import.validation_errors"
            )
        }

        return validationResult.map { CsvImportResult(0, emptyList(), emptyList()) }
    }

    private fun processImport(
        booklet: Booklet,
        rows: List<Array<String>>,
        userTags: List<Tag>,
        skipValidation: Boolean,
        month: Int?,
        year: Int?
    ): Result<CsvImportResult> {
        return unitOfWorkProvider.executeInTransaction(booklet) { bookletParam ->
            val results = convertRowsToTransactions(rows, userTags, skipValidation, month, year)
            val csvImportResult = saveTransactions(bookletParam, results)

            logger.info("CSV import completed: ${csvImportResult.successCount} transactions imported, ${csvImportResult.failedLines.size} errors")
            success(csvImportResult)
        }
    }

    private fun convertRowsToTransactions(
        rows: List<Array<String>>,
        userTags: List<Tag>,
        skipValidation: Boolean,
        month: Int?,
        year: Int?
    ): List<CsvLineResult> {
        val dataRows = rows.drop(1)
        return dataRows.mapIndexed { index, row ->
            val lineNumber = index + 2
            parseCsvLine(row, lineNumber)?.let { line ->
                if (skipValidation) validator.convertToTransaction(line, userTags, month, year)
                else validator.validateAndConvert(line, userTags, month, year)
            } ?: CsvLineResult.Error(lineNumber, listOf("Malformed CSV line"))
        }
    }

    private fun saveTransactions(bookletParam: Booklet, results: List<CsvLineResult>): CsvImportResult {
        val successResults = results.filterIsInstance<CsvLineResult.Success>()
        val errorResults = results.filterIsInstance<CsvLineResult.Error>()

        val savedTransactions = successResults.mapNotNull { result ->
            try {
                val bookletIdValue = bookletParam.id ?: run {
                    logger.warning("Booklet ID is null, cannot save transaction")
                    return@mapNotNull null
                }

                val savedTr = transactionRepository.save(bookletIdValue, result.transaction) ?: return@mapNotNull null

                bookletParam.addTransaction(savedTr)
                bookletRepository.update(bookletParam)
                savedTr
            } catch (e: Exception) {
                logger.warning("Error persisting transaction: ${e.message}")
                null
            }
        }

        return CsvImportResult(
            successCount = savedTransactions.size,
            failedLines = errorResults,
            transactions = savedTransactions
        )
    }


    private fun parseCsvLine(row: Array<String>, lineNumber: Int): CsvTransactionLine? {
        if (row.size != EXPECTED_COLUMN_COUNT) {
            logger.warning("Line $lineNumber skipped: invalid column count (${row.size} instead of $EXPECTED_COLUMN_COUNT)")
            return null
        }

        return CsvTransactionLine(
            lineNumber = lineNumber,
            date = row[DATE_COLUMN],
            label = row[LABEL_COLUMN],
            depense = row[DEPENSE_COLUMN],
            recette = row[RECETTE_COLUMN],
            tag = row[TAG_COLUMN]
        )
    }

    override fun exportTransactionsToCsv(
        token: String,
        transactions: List<Transaction>
    ): Result<String> {
        return sessionManager.authenticate(token) { _ ->
            try {
                val csvContent = csvExporter.exportToCsv(transactions)
                logger.info("CSV export completed: ${transactions.filter { !it.isPreview }.size} transactions exported")
                success(csvContent)
            } catch (e: Exception) {
                logger.severe("Error during CSV export: ${e.message}")
                domainFailure(
                    ResultState.INTERNAL_SERVER_ERROR,
                    "Error during export: ${e.message}",
                    "domain.file.export.internal_error"
                )
            }
        }
    }

}

