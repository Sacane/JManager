package fr.sacane.jmanager.domain.port.serverside

import fr.sacane.jmanager.common.hexadoc.PortToRight
import fr.sacane.jmanager.domain.model.*
import java.time.Month

@PortToRight
interface TransactionRegister {

    fun getSheets(user: UserId, accountLabel: String): List<Sheet>
    fun getSheetsByDateAndAccount(userId: UserId, month: Month, year: Int, labelAccount: String): List<Sheet>
    fun getAccounts(user: UserId): List<Account>?
    fun saveAccount(userId: UserId, account: Account)
    fun saveSheet(userId: UserId, accountLabel: String, sheet: Sheet): Boolean
    fun saveCategory(userId: UserId, category: Category): Boolean
    fun removeCategory(userId: UserId, labelCategory: String): Boolean
}
