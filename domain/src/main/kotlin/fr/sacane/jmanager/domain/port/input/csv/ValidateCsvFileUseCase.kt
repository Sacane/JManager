package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.csv.CsvValidationReport
import fr.sacane.jmanager.domain.utils.Result
import java.util.UUID

@Port(Side.APPLICATION)
interface ValidateCsvFileUseCase {
    fun validateCsvFile(
        token: SessionToken,
        bookletId: UUID,
        csvContent: String,
        month: Int? = null,
        year: Int? = null
    ): Result<CsvValidationReport>
}
