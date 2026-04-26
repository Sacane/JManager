package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface EditTransactionUseCase {
    fun handle(command: EditTransactionCommand): Result<TransactionResumeResult>
}
