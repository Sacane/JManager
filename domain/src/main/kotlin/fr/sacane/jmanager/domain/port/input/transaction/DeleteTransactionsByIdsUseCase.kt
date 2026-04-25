package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.api.TransactionDeletionResult
import fr.sacane.jmanager.domain.utils.Result
import java.util.UUID

@Port(Side.APPLICATION)
interface DeleteTransactionsByIdsUseCase {
    fun deleteTransactionsByIds(bookletID: UUID, transactionIds: List<UUID>, token: SessionToken): Result<TransactionDeletionResult>
}
