package fr.sacane.jmanager.domain.port.serverside
import fr.sacane.jmanager.domain.hexadoc.PortToRight
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Category
import fr.sacane.jmanager.domain.models.Sheet
import fr.sacane.jmanager.domain.models.UserId
import java.time.Month

@PortToRight
interface TransactionRegister {

    fun getSheets(user: UserId, accountLabel: String): List<Sheet>
    fun getSheetsByDateAndAccount(userId: UserId, month: Month, year: Int, labelAccount: String): List<Sheet>
    fun getAccounts(user: UserId): List<Account>?
    fun saveAccount(userId: UserId, account: Account): Account?
    fun saveSheet(userId: UserId, accountLabel: String, sheet: Sheet): Sheet?
    fun saveCategory(userId: UserId, category: Category): Category?
    fun removeCategory(userId: UserId, labelCategory: String): Category?
}
