package fr.sacane.jmanager.domain.port.spi
import fr.sacane.jmanager.domain.hexadoc.DomainSide
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*

@Port(DomainSide.DATASOURCE)
interface TransactionRegister {

    companion object{
        const val timeoutMessage: String = "Le temps d'utilisation du jeton d'authentification est écoulé"
        const val missingUserMessage: String = "L'utilisateur n'existe pas"
    }
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
    fun deleteAccountByID(accountID: Long)
    fun saveAllSheets(sheets: List<Sheet>)
    fun deleteAllSheetsById(sheetIds: List<Long>)
    fun findSheetByID(sheetID: Long): Sheet?
    fun save(sheet: Sheet): Sheet?
}
