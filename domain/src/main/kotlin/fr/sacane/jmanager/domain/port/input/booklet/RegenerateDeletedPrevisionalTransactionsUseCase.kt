package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.utils.Result
import java.time.Month
import java.util.UUID

@Port(Side.APPLICATION)
interface RegenerateDeletedPrevisionalTransactionsUseCase {
    fun regenerateDeletedPrevisionalTransactions(
        token: SessionToken,
        bookletId: UUID,
        month: Month,
        year: Int
    ): Result<List<Transaction>>
}
