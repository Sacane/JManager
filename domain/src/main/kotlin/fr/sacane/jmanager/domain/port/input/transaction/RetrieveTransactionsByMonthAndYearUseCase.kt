package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.utils.Result
import java.time.Month

@Port(Side.APPLICATION)
interface RetrieveTransactionsByMonthAndYearUseCase {
    fun retrieveTransactionsByMonthAndYear(token: SessionToken, month: Month, year: Int, bookletLabel: String): Result<List<Transaction>>
}
