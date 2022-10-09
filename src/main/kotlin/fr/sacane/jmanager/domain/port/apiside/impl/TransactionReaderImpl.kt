package fr.sacane.jmanager.domain.port.apiside.impl

import fr.sacane.jmanager.domain.model.*
import fr.sacane.jmanager.domain.port.apiside.TransactionReader
import fr.sacane.jmanager.domain.port.serverside.TransactionRegister
import java.time.Month

class TransactionReaderImpl(private val port: TransactionRegister): TransactionReader {

    override suspend fun registerUser(user: User): User {
        return port.saveUser(user)
    }

    override suspend fun findUserById(userId: UserId): User {
        return port.findUserById(userId)
    }

    override suspend fun findUserByPseudonym(pseudonym: String): User? {
        return port.findUserByPseudonym(pseudonym)
    }

    override suspend fun saveAccount(userId: UserId, account: Account) {
        return port.saveAccount(userId, account)
    }

    override suspend fun sheetByDateAndAccount(userId: UserId, month: Month, year: Int, account: String): List<Sheet> {
        return port.getSheetsByDateAndAccount(userId, month, year, account)
    }

    override suspend fun findAccount(userId: UserId, labelAccount: String): Account? {
        val user = port.findUserById(userId)
        return user.accounts().find { it.label() == labelAccount }
    }

    override suspend fun createUser(user: User): User? {
        return port.createUser(user)
    }

    override suspend fun saveSheet(userId: UserId, accountLabel: String, sheet: Sheet): Boolean {
        return port.saveSheet(userId, accountLabel, sheet)
    }

    override suspend fun checkUser(userId: String, pwd: String): Boolean {
        return port.checkUser(userId, Password(pwd))
    }

    override suspend fun getAccountByUser(userId: UserId): List<Account>?{
        return port.getAccounts(userId)
    }
}
