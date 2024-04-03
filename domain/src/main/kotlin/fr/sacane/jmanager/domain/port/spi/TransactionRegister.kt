package fr.sacane.jmanager.domain.port.spi
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*

@Port(Side.DATASOURCE)
interface TransactionRegister {

    companion object{
        const val timeoutMessage: String = "Le temps d'utilisation du jeton d'authentification est écoulé"
        const val missingUserMessage: String = "L'utilisateur n'existe pas"
    }
    fun persist(userId: UserId, account: Account): User?
    fun persist(userId: UserId, accountLabel: String, sheet: Sheet): Sheet?
    fun persist(userId: UserId, category: Tag): Tag?
    fun removeCategory(userId: UserId, labelCategory: String): Tag?
    fun findAccountByLabel(userId: UserId, labelAccount: String): Account?
    fun findAccountById(accountId: Long): Account?
    fun persist(account: Account) :Account?
    fun remove(targetCategory: Tag)
    fun deleteAccountByID(accountID: Long)
    fun saveAllSheets(sheets: List<Sheet>)
    fun deleteAllSheetsById(sheetIds: List<Long>)
    fun findSheetByID(sheetID: Long): Sheet?
    fun save(sheet: Sheet): Sheet?
    fun save(account: Account): Account?
    fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Account?
}
