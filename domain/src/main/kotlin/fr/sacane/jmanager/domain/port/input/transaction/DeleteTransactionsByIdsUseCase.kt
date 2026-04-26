package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.api.TransactionDeletionResult
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface DeleteTransactionsByIdsUseCase {
    fun handle(command: DeleteTransactionsByIdsCommand): Result<TransactionDeletionResult>
}
