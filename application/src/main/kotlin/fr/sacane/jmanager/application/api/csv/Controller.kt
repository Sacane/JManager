package fr.sacane.jmanager.application.api.csv

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.input.csv.*
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.application.api.currentUser
import fr.sacane.jmanager.application.api.toHttpResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

@RestController
@RequestMapping("api/csv")
@Adapter(Side.APPLICATION)
class CsvImportController(
    private val validateCsvFileUseCase: ValidateCsvFileUseCase,
    private val importTransactionsFromCsvUseCase: ImportTransactionsFromCsvUseCase,
    private val exportTransactionsToCsvUseCase: ExportTransactionsToCsvUseCase,
    private val transactionRepository: TransactionRepository
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

        return validateCsvFileUseCase.handle(
            ValidateCsvFileQuery(
                token = SessionToken(currentUser.token),
                bookletId = bookletId.toUUID(),
                csvContent = csvContent,
                month = month,
                year = year
            )
        ).map { it.toDTO() }.toHttpResponse()
    }

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

        return importTransactionsFromCsvUseCase.handle(
            ImportTransactionsFromCsvCommand(
                token = SessionToken(currentUser.token),
                bookletId = bookletId.toUUID(),
                csvContent = csvContent,
                skipValidation = skipValidation,
                month = month,
                year = year
            )
        ).map { it.toDTO() }.toHttpResponse()
    }

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

    @PostMapping("export", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun exportTransactionsToCsv(
        @Valid @RequestBody request: CsvExportRequestDTO
    ): ResponseEntity<*> {
        LOGGER.info("Exporting ${request.transactionIds.size} transactions to CSV")

        if (request.transactionIds.size > 10000) {
            LOGGER.warning("Export request exceeds maximum allowed transactions: ${request.transactionIds.size}")
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to "Le nombre de transactions à exporter ne peut pas dépasser 10 000"))
        }

        if (request.transactionIds.isEmpty()) {
            LOGGER.warning("Export request with empty transaction list")
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to "La liste des transactions à exporter ne peut pas être vide"))
        }

        try {
            val transactionIds = request.transactionIds.mapNotNull {
                try {
                    it.toUUID()
                } catch (e: Exception) {
                    LOGGER.warning("Invalid transaction ID format: $it")
                    null
                }
            }

            if (transactionIds.isEmpty()) {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapOf("message" to "Aucun ID de transaction valide fourni"))
            }

            val transactions = transactionIds.mapNotNull { id ->
                transactionRepository.findTransactionById(id)
            }

            if (transactions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapOf("message" to "Aucune transaction trouvée"))
            }

            val result = exportTransactionsToCsvUseCase.handle(
                ExportTransactionsToCsvCommand(
                    token = SessionToken(currentUser.token),
                    transactions = transactions
                )
            )

            return if (result.isSuccess()) {
                val csvContent = result.map { it }.mapNotNullOrFailure()
                if (csvContent != null) {
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    val filename = "transactions_export_$timestamp.csv"

                    LOGGER.info("CSV export successful: ${transactions.size} transactions exported to $filename")

                    ResponseEntity.ok()
                        .contentType(MediaType("text", "csv", StandardCharsets.UTF_8))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .header(HttpHeaders.EXPIRES, "0")
                        .body(csvContent.toByteArray(StandardCharsets.UTF_8))
                } else {
                    LOGGER.warning("CSV export returned null content")
                    ResponseEntity.internalServerError()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapOf("message" to "Erreur lors de l'export: contenu vide"))
                }
            } else {
                LOGGER.warning("CSV export failed: ${result.message}")
                ResponseEntity.status(result.status.code)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapOf("message" to result.message))
            }
        } catch (e: Exception) {
            LOGGER.severe("Unexpected error during CSV export: ${e.message}")
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to "Erreur lors de l'export: ${e.message}"))
        }
    }
}

