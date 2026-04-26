package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.csv.CsvValidationReport
import fr.sacane.jmanager.domain.port.spi.CsvFileReader
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.usecase.csv.CsvFileValidator
import fr.sacane.jmanager.domain.usecase.csv.CsvValidationUtils
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import java.util.UUID
import java.util.logging.Logger

@DomainService
class ValidateCsvFileService(
    private val csvFileReader: CsvFileReader,
    private val bookletRepository: BookletRepository,
    private val tagRepository: TagRepository,
    private val sessionManager: SessionManager
) : ValidateCsvFileUseCase {

    companion object {
        private val logger = Logger.getLogger(ValidateCsvFileService::class.java.name)
    }

    private val fileValidator = CsvFileValidator()

    override fun validateCsvFile(
        token: SessionToken,
        bookletId: UUID,
        csvContent: String,
        month: Int?,
        year: Int?
    ): Result<CsvValidationReport> {
        return sessionManager.authenticate(token) { userId ->
            val bookletFindResult = findBookletAndCheckOwner(bookletRepository, userId, bookletId)

            val userTags = tagRepository.getAllDefault(userId)
            val csvSeparator = CsvValidationUtils.detectCsvSeparator(csvContent)

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
                csvDomainFailure(
                    ResultState.INTERNAL_SERVER_ERROR,
                    "Error during validation: ${e.message}",
                    "domain.file.validation.internal_error"
                )
            }
        }
    }
}
