package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface DeleteRegularTransactionsUseCase {
    fun handle(command: DeleteRegularTransactionsCommand): Result<List<String>>
}
