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

        private val ALLOWED_CONTENT_TYPES = listOf(
            "text/csv",
            "text/plain",
            "application/csv",
            "application/vnd.ms-excel"
        )

        private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L
    }

    /**
     * Validates a CSV file before import
     *
     * This endpoint analyzes the CSV file structure and content to detect potential issues
     * such as format errors, missing data, or swapped columns.
     *
     * @param bookletId The booklet ID to import transactions into
     * @param file The CSV file to validate
     * @param month Optional month (1-12) to use when CSV date contains only day
     * @param year Optional year to use when CSV date contains only day
     * @return Validation report with warnings (success) or error details (failure)
     */
    @PostMapping("validate/{bookletId}")
    fun validateCsvFile(
        @PathVariable bookletId: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("month", required = false) month: Int?,
        @RequestParam("year", required = false) year: Int?
    ): ResponseEntity<*> {
        LOGGER.info("Validating CSV file '${file.originalFilename}' for booklet $bookletId (month=$month, year=$year)")

        val validationError = validateFileUpload(file)
        if (validationError != null) {
            return ResponseEntity.badRequest().body(mapOf("message" to validationError))
        }

        val csvContent = try {
            String(file.bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            LOGGER.warning("Error reading CSV file: ${e.message}")
            return ResponseEntity.badRequest().body(
                mapOf("message" to "Erreur d'encodage du fichier. Assurez-vous que le fichier est encodé en UTF-8")
            )
        }

        return csvImportFeature.validateCsvFile(
            token = currentUser.token,
            bookletId = bookletId.toUUID(),
            csvContent = csvContent,
            month = month,
            year = year
        ).map { it.toDTO() }.toHttpResponse()
    }

    /**
     * Imports transactions from a validated CSV file
     *
     * @param bookletId The booklet ID to import transactions into
     * @param file The CSV file to import
     * @param skipValidation If true, skips CSV validation (assumes it was already validated). Default: false for safety
     * @param month Optional month (1-12) to use when CSV date contains only day
     * @param year Optional year to use when CSV date contains only day
     * @return Import result with created transactions and potential errors
     */
    @PostMapping("import/{bookletId}")
    fun importTransactionsFromCsv(
        @PathVariable bookletId: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("skipValidation", defaultValue = "false", required = false) skipValidation: Boolean = false,
        @RequestParam("month", required = false) month: Int?,
        @RequestParam("year", required = false) year: Int?
    ): ResponseEntity<*> {
        LOGGER.info("Importing CSV file '${file.originalFilename}' for booklet $bookletId (month=$month, year=$year, skipValidation=$skipValidation)")

        val validationError = validateFileUpload(file)
        if (validationError != null) {
            return ResponseEntity.badRequest().body(mapOf("message" to validationError))
        }

        val csvContent = try {
            String(file.bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            LOGGER.warning("Error reading CSV file: ${e.message}")
            return ResponseEntity.badRequest().body(
                mapOf("message" to "Erreur d'encodage du fichier. Assurez-vous que le fichier est encodé en UTF-8")
            )
        }

        return csvImportFeature.importTransactionsFromCsv(
            token = currentUser.token,
            bookletId = bookletId.toUUID(),
            csvContent = csvContent,
            skipValidation = skipValidation,
            month = month,
            year = year
        ).map { it.toDTO() }.toHttpResponse()
    }

    /**
     * Validates the uploaded file (type, extension, size)
     *
     * @param file The file to validate
     * @return Error message if validation fails, null if valid
     */
    private fun validateFileUpload(file: MultipartFile): String? {
        if (file.isEmpty) {
            return "Le fichier est vide"
        }

        if (file.size > MAX_FILE_SIZE_BYTES) {
            val maxSizeMb = MAX_FILE_SIZE_BYTES / (1024 * 1024)
            return "Le fichier est trop volumineux (${file.size / (1024 * 1024)} Mo). Taille maximale: $maxSizeMb Mo"
        }

        val filename = file.originalFilename ?: ""
        if (!filename.endsWith(".csv", ignoreCase = true)) {
            return "Extension de fichier non supportée. Seuls les fichiers .csv sont acceptés"
        }

        val contentType = file.contentType
        if (contentType != null && contentType !in ALLOWED_CONTENT_TYPES) {
            LOGGER.warning("Suspicious content type: $contentType for file: $filename")
        }

        return null
    }
}

