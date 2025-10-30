package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.csv.CsvImportResult
import fr.sacane.jmanager.domain.models.csv.CsvTransactionLine
import fr.sacane.jmanager.domain.models.csv.CsvLineResult
import fr.sacane.jmanager.domain.models.csv.CsvValidationReport
import fr.sacane.jmanager.domain.port.spi.CsvFileReader
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.csv.CsvTransactionValidator
import fr.sacane.jmanager.domain.usecase.csv.CsvFileValidator
import fr.sacane.jmanager.domain.utils.*
import java.util.*
import java.util.logging.Logger

@Port(Side.APPLICATION)
/**
 * Application port: CsvImportFeature
 *
 * Allows importing transactions from a CSV file.
 * The CSV file must contain the following columns:
 * - date (format dd-MM-yyyy)
 * - label
 * - depense (expense)
 * - recette (income)
 * - tag
 */
sealed interface CsvImportFeature {

    /**
     * Validates CSV content before import
     *
     * @param token Authentication token
     * @param bookletId ID of the booklet to import transactions into
     * @param csvContent CSV file content (raw text)
     * @return Result containing CsvValidationReport with warnings or error with first critical issue
     */
    fun validateCsvFile(
        token: String,
        bookletId: UUID,
        csvContent: String
    ): Result<CsvValidationReport>

    /**
     * Imports transactions from CSV content for a given booklet
     *
     * @param token Authentication token
     * @param bookletId ID of the booklet to import transactions into
     * @param csvContent CSV file content (raw text)
     * @param skipValidation If true, skips CSV validation (assumes it was already validated). Default: false for safety
     * @return Result containing CsvImportResult with created transactions and potential errors
     */
    fun importTransactionsFromCsv(
        token: String,
        bookletId: UUID,
        csvContent: String,
        skipValidation: Boolean = false
    ): Result<CsvImportResult>
}

@DomainService
class CsvImportFeatureImpl(
    private val csvFileReader: CsvFileReader,
    private val transactionRepository: TransactionRepository,
    private val bookletRepository: BookletRepository,
    private val tagRepository: TagRepository,
    private val sessionManager: SessionManager,
    private val unitOfWorkProvider: UnitOfWorkTransactionProvider
) : CsvImportFeature {

    companion object {
        private val logger = Logger.getLogger(CsvImportFeatureImpl::class.java.name)

        private const val DATE_COLUMN = 0
        private const val LABEL_COLUMN = 1
        private const val DEPENSE_COLUMN = 2
        private const val RECETTE_COLUMN = 3
        private const val TAG_COLUMN = 4
        private const val EXPECTED_COLUMN_COUNT = 5
    }

    private val validator = CsvTransactionValidator()
    private val fileValidator = CsvFileValidator()

    override fun validateCsvFile(
        token: String,
        bookletId: UUID,
        csvContent: String
    ): Result<CsvValidationReport> {
        return sessionManager.authenticate(token) { userId ->
            val booklet = bookletRepository.findAccountByIdWithTransactions(bookletId)
                ?: return@authenticate notFound("Booklet with id '$bookletId' does not exist")

            if (booklet.owner?.id?.value != userId.value) {
                return@authenticate forbidden("You do not have access to this booklet")
            }

            val userTags = tagRepository.getAllDefault(userId)

            try {
                val rows = csvFileReader.readCsvContent(csvContent)
                val validationResult = fileValidator.validate(rows, userTags)
                
                validationResult.mapNullable { report ->
                    if (report != null) {
                        logger.info("CSV validation completed: ${report.totalLines} lines, ${report.validLines} valid, ${report.warnings.size} warnings")
                    } else {
                        logger.warning("CSV validation failed: ${validationResult.message}")
                    }
                }

                validationResult
            } catch (e: Exception) {
                logger.severe("Error during CSV validation: ${e.message}")
                failure(ResultState.INTERNAL_SERVER_ERROR, "Error during validation: ${e.message}")
            }
        }
    }

    override fun importTransactionsFromCsv(
        token: String,
        bookletId: UUID,
        csvContent: String,
        skipValidation: Boolean
    ): Result<CsvImportResult> {
        return sessionManager.authenticate(token) { userId ->
            val booklet = bookletRepository.findAccountByIdWithTransactions(bookletId)
                ?: return@authenticate notFound("Booklet with id '$bookletId' does not exist")

            if (booklet.owner?.id?.value != userId.value) {
                return@authenticate forbidden("You do not have access to this booklet")
            }

            val userTags = tagRepository.getAllDefault(userId)

            try {
                val rows = csvFileReader.readCsvContent(csvContent)

                // Validation conditionnelle : seulement si skipValidation = false
                if (!skipValidation) {
                    val validationResult = fileValidator.validate(rows, userTags)

                    if (validationResult.isFailure()) {
                        return@authenticate failure(validationResult.status, validationResult.message)
                    }

                    // Check for validation errors before proceeding
                    val hasErrors = validationResult.mapNullable { report ->
                        report?.hasErrors ?: false
                    }

                    if (hasErrors) {
                        val errorMessages = validationResult.mapNullable { report ->
                            report?.errors?.joinToString("; ") {
                                "Line ${it.lineNumber}: ${it.message}"
                            } ?: "Unknown validation errors"
                        }
                        return@authenticate invalid("CSV validation failed: $errorMessages")
                    }
                }

                // UnitOfWork utilisé ici car on sauvegarde potentiellement des centaines de transactions
                // Si une erreur survient pendant la sauvegarde, on doit ROLLBACK toutes les transactions
                // pour éviter un import partiel incohérent
                unitOfWorkProvider.executeInTransaction(booklet) { bookletParam ->
                    val dataRows = rows.drop(1)
                    val results = dataRows.mapIndexed { index, row ->
                        val lineNumber = index + 2
                        parseCsvLine(row, lineNumber)?.let { line ->
                            // Conversion simple si skipValidation = true, sinon on utilise la méthode sûre
                            if (skipValidation) {
                                validator.convertToTransaction(line, userTags)
                            } else {
                                validator.convertToTransaction(line, userTags)
                            }
                        } ?: CsvLineResult.Error(lineNumber, listOf("Malformed CSV line"))
                    }

                    val successResults = results.filterIsInstance<CsvLineResult.Success>()
                    val errorResults = results.filterIsInstance<CsvLineResult.Error>()

                    val savedTransactions = successResults.mapNotNull { result ->
                        try {
                            val bookletIdValue = bookletParam.id ?: run {
                                logger.warning("Booklet ID is null, cannot save transaction")
                                return@mapNotNull null
                            }
                            transactionRepository.save(bookletIdValue, result.transaction)
                        } catch (e: Exception) {
                            logger.warning("Error persisting transaction: ${e.message}")
                            null
                        }
                    }

                    val csvImportResult = CsvImportResult(
                        successCount = savedTransactions.size,
                        failedLines = errorResults,
                        transactions = savedTransactions
                    )

                    logger.info("CSV import completed: ${csvImportResult.successCount} transactions imported, ${csvImportResult.failedLines.size} errors")

                    success(csvImportResult)
                }
            } catch (e: Exception) {
                logger.severe("Error during CSV import: ${e.message}")
                failure(ResultState.INTERNAL_SERVER_ERROR, "Error during import: ${e.message}")
            }
        }
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
}

