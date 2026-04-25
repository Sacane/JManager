package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.csv.CsvImportResult
import fr.sacane.jmanager.domain.utils.Result
import java.util.UUID

@Port(Side.APPLICATION)
interface ImportTransactionsFromCsvUseCase {
    fun importTransactionsFromCsv(
        token: SessionToken,
        bookletId: UUID,
        csvContent: String,
        skipValidation: Boolean,
        month: Int?,
        year: Int?
    ): Result<CsvImportResult>
}
