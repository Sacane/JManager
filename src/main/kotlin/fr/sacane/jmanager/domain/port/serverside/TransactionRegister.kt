package fr.sacane.jmanager.domain.port.serverside

import fr.sacane.jmanager.domain.model.*
import java.time.Month


interface TransactionRegister {

    fun getSheets(user: UserId, accountLabel: String): List<Sheet>
    fun getSheetsByDateAndAccount(userId: UserId, month: Month, year: Int, labelAccount: String): List<Sheet>
    fun getAccounts(user: UserId): List<Account>?
    fun saveUser(user: User): User
    fun findUserById(userId: UserId): User
    fun saveAccount(userId: UserId, account: Account)
    fun findUserByPseudonym(pseudonym: String): User?
    fun createUser(user: User): User?
    fun saveSheet(userId: UserId, accountLabel: String, sheet: Sheet): Boolean
    fun checkUser(pseudonym: String, pwd: Password): Boolean
    fun saveCategory(userId: UserId, category: Category): Boolean
    fun retrieveAllCategory(userId: Long): List<Category>
    fun removeCategory(userId: UserId, labelCategory: String): Boolean
}
