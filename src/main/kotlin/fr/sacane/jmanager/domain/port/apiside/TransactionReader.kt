package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.domain.model.*
import java.time.Month

//TODO split in several files because this is weird
interface TransactionReader {

    fun saveAccount(userId: UserId, account: Account)
    fun sheetByDateAndAccount(userId: UserId, month: Month, year: Int, account: String): List<Sheet>
    fun findAccount(userId: UserId, token: Token, labelAccount: String): Response<Account>
    fun saveSheet(userId: UserId, token: Token, accountLabel: String, sheet: Sheet): Boolean
    fun addCategory(userId: UserId, token: Token, category: Category): Boolean
    fun getAccountByUser(userId: UserId): List<Account>?
    fun retrieveAllCategoryOfUser(userId: Long): List<Category>
    fun removeCategory(id: UserId, label: String): Boolean
}
