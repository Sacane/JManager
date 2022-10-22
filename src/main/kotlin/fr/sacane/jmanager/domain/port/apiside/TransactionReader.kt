package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.domain.model.*
import java.time.Month

//TODO split in several files because this is weird
interface TransactionReader {

    suspend fun registerUser(user: User): User
    suspend fun findUserById(userId: UserId): User
    suspend fun findUserByPseudonym(pseudonym: String): User?
    suspend fun saveAccount(userId: UserId, account: Account)
    suspend fun sheetByDateAndAccount(userId: UserId, month: Month, year: Int, account: String): List<Sheet>
    suspend fun findAccount(userId: UserId, labelAccount: String): Account?
    suspend fun createUser(user: User): User?
    suspend fun saveSheet(userId: UserId, accountLabel: String, sheet: Sheet): Boolean
    suspend fun addCategory(userId: UserId, category: Category): Boolean
    suspend fun checkUser(userId: String, pwd:String): Boolean
    suspend fun getAccountByUser(userId: UserId): List<Account>?
    suspend fun retrieveAllCategoryOfUser(userId: Long): List<Category>
    suspend fun removeCategory(id: UserId, label: String): Boolean
}
