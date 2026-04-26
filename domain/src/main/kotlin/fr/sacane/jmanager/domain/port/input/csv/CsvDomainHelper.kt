package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.csv.CsvImportResult
import fr.sacane.jmanager.domain.models.csv.CsvLineResult
import fr.sacane.jmanager.domain.models.csv.CsvTransactionLine
import fr.sacane.jmanager.domain.models.csv.CsvValidationReport
import fr.sacane.jmanager.domain.port.spi.CsvFileReader
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.domain.usecase.csv.CsvFileValidator
import fr.sacane.jmanager.domain.usecase.csv.CsvTransactionValidator
import fr.sacane.jmanager.domain.usecase.csv.CsvValidationUtils
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID
import java.util.logging.Logger

private val logger = Logger.getLogger("CsvDomainHelper")

private const val DATE_COLUMN = 0
private const val LABEL_COLUMN = 1
private const val DEPENSE_COLUMN = 2
private const val RECETTE_COLUMN = 3
private const val TAG_COLUMN = 4
private const val EXPECTED_COLUMN_COUNT = 5

internal fun <S> csvDomainFailure(state: ResultState, detail: String, key: String): Result<S> {
    return failure(state, DomainError(state.code, key, detail))
}

internal fun findBookletAndCheckOwner(bookletRepository: BookletRepository, userId: UserId, bookletId: UUID): Result<Booklet> {
    val booklet =
        bookletRepository.findBookletByIdWithTransactions(bookletId)
            ?: return csvDomainFailure(
                ResultState.NOT_FOUND,
                "Booklet not found",
                "domain.file.booklet.not_found"
            )
    if (booklet.owner?.id?.value != userId.value) {
        return csvDomainFailure(
            ResultState.FORBIDDEN,
            "You are not the owner of this booklet",
            "domain.file.booklet.forbidden_owner"
        )
    }
    return success(booklet)
}

internal fun checkValidationErrors(fileValidator: CsvFileValidator, rows: List<Array<String>>, userTags: List<Tag>, month: Int?, year: Int?, csvSeparator: Char?): Result<CsvImportResult> {
    val validationResult = fileValidator.validate(rows, userTags, month, year, csvSeparator)

    if (validationResult.isFailure()) {
        return csvDomainFailure(
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
        return csvDomainFailure(
            ResultState.INVALID,
            "CSV validation failed: $errorMessages",
            "domain.file.import.validation_errors"
        )
    }

    return validationResult.map { CsvImportResult(0, emptyList(), emptyList()) }
}

internal fun convertRowsToTransactions(
    validator: CsvTransactionValidator,
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

internal fun saveTransactions(
    transactionRepository: TransactionRepository,
    bookletRepository: BookletRepository,
    bookletParam: Booklet,
    results: List<CsvLineResult>
): CsvImportResult {
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
