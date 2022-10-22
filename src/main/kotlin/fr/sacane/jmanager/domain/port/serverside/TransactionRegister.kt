package fr.sacane.jmanager.domain.port.serverside

import fr.sacane.jmanager.domain.model.*
import java.time.Month


interface TransactionRegister {

    suspend fun getSheets(user: UserId, accountLabel: String): List<Sheet>
    suspend fun getSheetsByDateAndAccount(userId: UserId, month: Month, year: Int, labelAccount: String): List<Sheet>
    suspend fun getAccounts(user: UserId): List<Account>?
    suspend fun saveUser(user: User): User
    suspend fun findUserById(userId: UserId): User
    suspend fun saveAccount(userId: UserId, account: Account)
    suspend fun findUserByPseudonym(pseudonym: String): User?
    suspend fun createUser(user: User): User?
    suspend fun saveSheet(userId: UserId, accountLabel: String, sheet: Sheet): Boolean
    suspend fun checkUser(pseudonym: String, pwd: Password): Boolean
    suspend fun saveCategory(userId: UserId, category: Category): Boolean
    suspend fun retrieveAllCategory(userId: Long): List<Category>
    suspend fun removeCategory(userId: UserId, labelCategory: String): Boolean
}
