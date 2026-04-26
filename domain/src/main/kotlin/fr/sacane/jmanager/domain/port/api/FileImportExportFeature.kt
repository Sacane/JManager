package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.csv.CsvImportResult
import fr.sacane.jmanager.domain.models.csv.CsvValidationReport
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.utils.Result
import java.util.UUID

@Deprecated("Use individual use case interfaces from domain.port.input.csv instead")
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
        token: SessionToken,
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
        token: SessionToken,
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
        token: SessionToken,
        transactions: List<Transaction>
    ): Result<String>
}



