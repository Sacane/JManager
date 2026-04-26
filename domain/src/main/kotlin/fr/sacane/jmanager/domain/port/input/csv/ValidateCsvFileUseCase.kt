package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.csv.CsvValidationReport
import fr.sacane.jmanager.domain.utils.Result
import java.util.UUID

data class ValidateCsvFileQuery(
    val token: SessionToken,
    val bookletId: UUID,
    val csvContent: String,
    val month: Int? = null,
    val year: Int? = null
)

@Port(Side.APPLICATION)
interface ValidateCsvFileUseCase {
    fun handle(query: ValidateCsvFileQuery): Result<CsvValidationReport>
}
