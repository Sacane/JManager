package fr.sacane.jmanager.domain.port.input.csv

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface ExportTransactionsToCsvUseCase {
    fun exportTransactionsToCsv(
        token: SessionToken,
        transactions: List<Transaction>
    ): Result<String>
}
