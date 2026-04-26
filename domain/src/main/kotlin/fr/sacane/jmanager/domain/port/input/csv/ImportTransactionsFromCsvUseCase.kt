package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.csv.CsvImportResult
import fr.sacane.jmanager.domain.utils.Result
import java.util.UUID

data class ImportTransactionsFromCsvCommand(
    val token: SessionToken,
    val bookletId: UUID,
    val csvContent: String,
    val skipValidation: Boolean = false,
    val month: Int? = null,
    val year: Int? = null
)

@Port(Side.APPLICATION)
interface ImportTransactionsFromCsvUseCase {
    fun handle(command: ImportTransactionsFromCsvCommand): Result<CsvImportResult>
}
