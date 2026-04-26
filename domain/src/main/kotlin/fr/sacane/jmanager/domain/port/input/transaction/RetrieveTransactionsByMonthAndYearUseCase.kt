package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface RetrieveTransactionsByMonthAndYearUseCase {
    fun handle(query: RetrieveTransactionsByMonthAndYearQuery): Result<List<Transaction>>
}
