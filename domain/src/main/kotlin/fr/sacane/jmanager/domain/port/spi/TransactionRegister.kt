package fr.sacane.jmanager.domain.port.spi
import fr.sacane.jmanager.domain.hexadoc.RightPort
import fr.sacane.jmanager.domain.models.*
import java.time.Month

@RightPort
interface TransactionRegister {

    fun persist(userId: UserId, account: Account): User?
    fun persist(userId: UserId, accountLabel: String, sheet: Sheet): Sheet?
    fun persist(userId: UserId, category: Category): Category?
    fun removeCategory(userId: UserId, labelCategory: String): Category?
    fun findAccountByLabel(userId: UserId, labelAccount: String): Account?
    fun findAccountById(accountId: Long): Account?
//    fun remove(targetCategory: Category)
    fun persist(account: Account) :Account?
    fun remove(targetCategory: Category)
    fun deleteAllSheets(accountID: Long, sheets: List<Long>)
}
