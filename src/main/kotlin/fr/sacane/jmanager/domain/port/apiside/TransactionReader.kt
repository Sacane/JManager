package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.domain.model.*
import java.time.Month

//TODO split in several files because this is weird
interface TransactionReader {

    fun registerUser(user: User): User //TODO REMOVE
    fun findUserById(userId: UserId): User //TODO REMOVE
    fun findUserByPseudonym(pseudonym: String): User?
    fun saveAccount(userId: UserId, account: Account)
    fun sheetByDateAndAccount(userId: UserId, month: Month, year: Int, account: String): List<Sheet>
    fun findAccount(userId: UserId, labelAccount: String): Account?
    fun createUser(user: User): User? //TODO REMOVE
    fun saveSheet(userId: UserId, accountLabel: String, sheet: Sheet): Boolean
    fun addCategory(userId: UserId, category: Category): Boolean
    fun checkUser(userId: String, pwd:String): Boolean //TODO REMOVE
    fun getAccountByUser(userId: UserId): List<Account>?
    fun retrieveAllCategoryOfUser(userId: Long): List<Category>
    fun removeCategory(id: UserId, label: String): Boolean
}
