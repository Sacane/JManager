package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.csv.CsvValidationReport
import fr.sacane.jmanager.domain.port.output.CsvFileReader
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.domain.usecase.csv.CsvFileValidator
import fr.sacane.jmanager.domain.usecase.csv.CsvValidationUtils
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import java.util.UUID
import java.util.logging.Logger

data class ValidateCsvFileQuery(
    val userId: UserId,
    val bookletId: UUID,
    val csvContent: String,
    val month: Int? = null,
    val year: Int? = null
) : Query<CsvValidationReport>

@Port(Side.APPLICATION)
interface ValidateCsvFileUseCase : QueryHandler<ValidateCsvFileQuery, CsvValidationReport> {
    override val queryClass get() = ValidateCsvFileQuery::class
}

@DomainService
class ValidateCsvFileService(
    private val csvFileReader: CsvFileReader,
    private val bookletRepository: BookletRepository,
    private val tagRepository: TagRepository
) : ValidateCsvFileUseCase {

    companion object {
        private val logger = Logger.getLogger(ValidateCsvFileService::class.java.name)
    }

    private val fileValidator = CsvFileValidator()

    override fun handle(query: ValidateCsvFileQuery): Result<CsvValidationReport> {
        val userId = query.userId
        val bookletFindResult = findBookletAndCheckOwner(bookletRepository, userId, query.bookletId)

        val userTags = tagRepository.getAllDefault(userId)
        val csvSeparator = CsvValidationUtils.detectCsvSeparator(query.csvContent)

        try {
            return bookletFindResult.flatMap {
                    val rows = csvFileReader.readCsvContent(query.csvContent)
                val validationResult = fileValidator.validate(rows, userTags, query.month, query.year, csvSeparator)

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
            return csvDomainFailure(
                ResultState.INTERNAL_SERVER_ERROR,
                "Error during validation: ${e.message}",
                "domain.file.validation.internal_error"
            )
        }
    }
}
