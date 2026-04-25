package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.PrevisionalTransactionsOutput
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.utils.Result
import java.time.LocalDate
import java.util.UUID

@Port(Side.APPLICATION)
interface GetPrevisionalTransactionsUseCase {
    fun getPrevisionalTransactions(
        token: SessionToken,
        startDate: LocalDate,
        endDate: LocalDate,
        bookletId: UUID?
    ): Result<PrevisionalTransactionsOutput>
}
