package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface DeleteRegularTransactionUseCase {
    fun handle(command: DeleteRegularTransactionCommand): Result<Boolean>
}
