package fr.sacane.jmanager.infrastructure.api.csv

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.api.CsvImportFeature
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.infrastructure.api.currentUser
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.logging.Logger

/**
 * REST Controller for CSV import operations
 */
@RestController
@RequestMapping("api/csv")
@Adapter(Side.APPLICATION)
class CsvImportController(
    private val csvImportFeature: CsvImportFeature
) {
    companion object {
        private val LOGGER: Logger = Logger.getLogger("CsvImportController")
    }

    /**
     * Validates a CSV file before import
     *
     * This endpoint analyzes the CSV file structure and content to detect potential issues
     * such as format errors, missing data, or swapped columns.
     *
     * @param bookletId The booklet ID to import transactions into
     * @param file The CSV file to validate
     * @return Validation report with warnings (success) or error details (failure)
     */
    @PostMapping("validate/{bookletId}")
    fun validateCsvFile(
        @PathVariable bookletId: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<CsvValidationReportDTO> {
        LOGGER.info("Validating CSV file for booklet $bookletId")

        if (file.isEmpty) {
            return ResponseEntity.badRequest().build()
        }

        val csvContent = String(file.bytes, Charsets.UTF_8)

        return csvImportFeature.validateCsvFile(
            token = currentUser.token,
            bookletId = bookletId.toUUID(),
            csvContent = csvContent
        ).map { it.toDTO() }.toHttpResponse()
    }

    /**
     * Imports transactions from a validated CSV file
     *
     * @param bookletId The booklet ID to import transactions into
     * @param file The CSV file to import
     * @return Import result with created transactions and potential errors
     */
    @PostMapping("import/{bookletId}")
    fun importTransactionsFromCsv(
        @PathVariable bookletId: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<CsvImportResultDTO> {
        LOGGER.info("Importing CSV file for booklet $bookletId")

        if (file.isEmpty) {
            return ResponseEntity.badRequest().build<CsvImportResultDTO>()
        }

        val csvContent = String(file.bytes, Charsets.UTF_8)

        return csvImportFeature.importTransactionsFromCsv(
            token = currentUser.token,
            bookletId = bookletId.toUUID(),
            csvContent = csvContent
        ).map { it.toDTO() }.toHttpResponse()
    }
}

