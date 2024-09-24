package fr.sacane.jmanager.domain.port.spi
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*

@Port(Side.INFRASTRUCTURE)
interface TransactionRepositoryPort {
    fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction?
    fun findAccountByLabel(userId: UserId, labelAccount: String): Account?
    fun findAccountById(accountId: Long): Account?
    fun saveAllSheets(transactions: List<Transaction>)
    fun deleteAllSheetsById(sheetIds: List<Long>)
    fun findTransactionById(transactionId: Long): Transaction?
    fun save(transaction: Transaction): Transaction?
    fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Account?
}
