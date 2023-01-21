package fr.sacane.jmanager.domain.port.apiside.impl

import fr.sacane.jmanager.domain.model.*
import fr.sacane.jmanager.domain.port.apiside.TransactionReader
import fr.sacane.jmanager.domain.port.apiside.UserRegisterFlow
import fr.sacane.jmanager.domain.port.serverside.TransactionRegister
import fr.sacane.jmanager.domain.port.serverside.UserTransaction
import java.time.Month

//TODO put it into TransactionReader without separate in many files
class TransactionReaderImpl(private val port: TransactionRegister, private val userPort: UserTransaction): TransactionReader {


    override fun saveAccount(userId: UserId, account: Account) {
        return port.saveAccount(userId, account)
    }

    override fun sheetByDateAndAccount(userId: UserId, month: Month, year: Int, account: String): List<Sheet> {
        return port.getSheetsByDateAndAccount(userId, month, year, account)
    }

    override fun findAccount(userId: UserId, labelAccount: String): Account? {
        val user = userPort.findById(userId)
        return user.accounts().find { it.label() == labelAccount }
    }


    override fun saveSheet(userId: UserId, accountLabel: String, sheet: Sheet): Boolean {
        return port.saveSheet(userId, accountLabel, sheet)
    }

    override fun addCategory(userId: UserId, category: Category): Boolean {
        return port.saveCategory(userId, category)
    }

    override fun getAccountByUser(userId: UserId): List<Account>?{
        return port.getAccounts(userId)
    }

    override fun retrieveAllCategoryOfUser(userId: Long): List<Category> {
        return port.retrieveAllCategory(userId)
    }

    override fun removeCategory(id: UserId, label: String): Boolean {
        return port.removeCategory(id, label)
    }
}
