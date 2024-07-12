package fr.sacane.jmanager.domain.port.spi
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*

@Port(Side.INFRASTRUCTURE)
interface TransactionRegister {
    fun persist(userId: UserId, account: Account): User?
    fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction?
    fun persist(userId: UserId, category: Tag): Tag?
    fun findAccountByLabel(userId: UserId, labelAccount: String): Account?
    fun findAccountById(accountId: Long): Account?
    fun persist(account: Account) :Account?
    fun remove(targetCategory: Tag)
    fun deleteAccountByID(accountID: Long)
    fun saveAllSheets(transactions: List<Transaction>)
    fun deleteAllSheetsById(sheetIds: List<Long>)
    fun findSheetByID(sheetID: Long): Transaction?
    fun save(transaction: Transaction): Transaction?
    fun save(account: Account): Account?
    fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Account?
}
