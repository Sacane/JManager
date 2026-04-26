package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface RegenerateDeletedPrevisionalTransactionsUseCase {
    fun handle(command: RegenerateDeletedPrevisionalTransactionsCommand): Result<List<Transaction>>
}
