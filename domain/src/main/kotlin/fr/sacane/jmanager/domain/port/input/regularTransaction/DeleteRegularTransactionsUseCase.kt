package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface DeleteRegularTransactionsUseCase {
    fun deleteRegularTransactions(token: SessionToken, transactionIds: List<String>): Result<List<String>>
}
