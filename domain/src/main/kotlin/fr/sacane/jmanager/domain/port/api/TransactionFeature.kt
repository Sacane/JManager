package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.AccountRepository
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.time.Month
import java.util.*

@Port(Side.APPLICATION)
sealed interface TransactionFeature {
    fun saveAndLink(userId: UserId, token: UUID, accountLabel: String, transaction: Transaction): Response<Transaction>
    fun retrieveSheetsByMonthAndYear(userId: UserId, token: UUID, month: Month, year: Int, account: String): Response<List<Transaction>>
    fun editSheet(userID: Long, accountID: Long, transaction: Transaction, token: UUID): Response<Transaction>
    fun findById(userID: Long, id: Long, token: UUID): Response<Transaction>
    fun deleteSheetsByIds(userId: UserId, accountID: Long, sheetIds: List<Long>, token: UUID)
}

@DomainService
class TransactionFeatureImpl(
    private val register: TransactionRegister,
    private val userRepository: UserRepository,
    private val session: InMemorySessionManager,
    private val accountRepository: AccountRepository
): TransactionFeature{

    private fun updateSheetSoldFrom(account: Account, month: Month, update: Boolean = true){
        val sheets = account.transactions.filter { it.date.month == month }
        for(number in sheets.indices) {
            if(number == 0) continue
            sheets[number].position = sheets[number - 1].position
        }
        register.saveAllSheets(
            sheets.map{ sheet ->
                val lastRecord = sheets.find { it.position == sheet.position - 1 }
                sheet.also {
                    if(lastRecord == null) {
                        if(update) sheet.accountAmount = account.sold
                    } else {
                        sheet.updateSoldStartingWith(lastRecord.accountAmount)
                    }
                }
            }.toList()
        )
    }
    private fun updateSheetPosition(accountID: Long, year: Int, month: Month) {
        val account = register.findAccountById(accountID)
        val sheets = account?.transactions
            ?.filter { it.date.year == year && it.date.month == month }
            ?.sortedBy { it.position }
            ?: return

        for(number in sheets.indices) {
            sheets[number].position = number
        }
        register.saveAllSheets(sheets)
    }

    override fun saveAndLink(
        userId: UserId,
        token: UUID,
        accountLabel: String,
        transaction: Transaction
    ): Response<Transaction> = session.authenticate(userId, token) {
        val account = register.findAccountWithSheetByLabelAndUser(accountLabel, userId) ?: return@authenticate Response.notFound("Le compte $accountLabel n'existe pas")
        if(account.transactions.isNotEmpty()) {
            val lastRecord = account.transactions
                .filter { it.date <= transaction.date }
                .maxByOrNull { it.position }
            if(lastRecord == null) {
                transaction.position = 0
                transaction.updateSoldStartingWith(account.sold)
            } else {
                transaction.position = lastRecord.position + 1
                transaction.updateSoldStartingWith(lastRecord.accountAmount)
            }
            updateSheetPosition(account.id!!, transaction.date.year, transaction.date.month)
        } else {
            transaction.updateSoldStartingWith(account.sold)
        }
        register.persist(userId, accountLabel, transaction) ?: return@authenticate Response.invalid("Une erreur est survenue lors de l'ajout de la transaction")
        Response.ok(transaction)
    }

    override fun retrieveSheetsByMonthAndYear(
        userId: UserId,
        token: UUID,
        month: Month,
        year: Int,
        account: String
    ): Response<List<Transaction>> = session.authenticate(userId, token) {
        val user = userRepository.findUserById(userId) ?: return@authenticate Response.notFound("The user does not exists")
        Response.ok(register.findAccountWithSheetByLabelAndUser(account, user.id)
            ?.retrieveSheetSurroundByDate(month, year)
            ?: return@authenticate Response.notFound("Aucun compte ne correspond au label indiqué")
        )
    }

    override fun editSheet(
        userID: Long,
        accountID: Long,
        transaction: Transaction,
        token: UUID
    ): Response<Transaction> = session.authenticate(UserId(userID), token, roleUser) {
        if(transaction.id == null) return@authenticate Response.invalid("L'ID de la transaction est null")
        val acc = register.findAccountById(accountID)
        val sheetFromResource = acc?.transactions?.find { it.id == transaction.id } ?: return@authenticate Response.notFound("Aucune transaction n'existe avec l'ID suivant : ${transaction.id}")
        acc.updateSoldFromTransactions(sheetFromResource, transaction)
        sheetFromResource.updateFromOther(transaction)
        sheetFromResource.accountAmount = acc.sold
        register.save(sheetFromResource)
        updateSheetSoldFrom(acc, transaction.date.month, false)

        //acc.updateSoldByLastSheet()
        return@authenticate accountRepository.editFromAnother(acc).run {
            this ?: return@authenticate Response.invalid("Une erreur est survenu lors de la sauvegarde de la transaction")
            Response.ok(sheetFromResource)
        }
    }

    override fun findById(
        userID: Long,
        id: Long,
        token: UUID
    ): Response<Transaction> = session.authenticate(UserId(userID), token, roleUser) {
        val sheet = register.findSheetByID(id) ?: return@authenticate Response.notFound("La transaction n'existe pas")
        Response.ok(sheet)
    }

    override fun deleteSheetsByIds(userId: UserId, accountID: Long, sheetIds: List<Long>, token: UUID) {
        val account: Account = register.findAccountById(accountID) ?: return
        val isSheetOnList: (s: Transaction) -> Boolean = { sheetIds.contains(it.id) }
        val month = account.transactions.filter(isSheetOnList)[0].date.month
        account.cancelSheetsAmount(account.transactions.filter(isSheetOnList))
        account.transactions.removeIf(isSheetOnList)
        updateSheetSoldFrom(account, month)
        register.persist(account)
        register.deleteAllSheetsById(sheetIds)
    }

}
