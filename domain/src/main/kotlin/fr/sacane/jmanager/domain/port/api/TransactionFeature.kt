package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.AccountRepository
import fr.sacane.jmanager.domain.port.spi.TransactionRepositoryPort
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.time.LocalDateTime
import java.time.Month
import java.util.*
import java.util.logging.Logger

@Port(Side.APPLICATION)
sealed interface TransactionFeature {
    fun bookTransaction(userId: UserId, token: UUID, accountLabel: String, transaction: Transaction): Response<Transaction>
    fun retrieveTransactionsByMonthAndYear(userId: UserId, token: UUID, month: Month, year: Int, account: String): Response<List<Transaction>>
    fun editTransaction(userID: Long, accountID: Long, transaction: Transaction, token: UUID): Response<Transaction>
    fun findById(userID: Long, id: Long, token: UUID): Response<Transaction>
    fun deleteSheetsByIds(userId: UserId, accountID: Long, sheetIds: List<Long>, token: UUID)
}

@DomainService
class TransactionFeatureImpl(
    private val transactionRepository: TransactionRepositoryPort,
    private val userRepository: UserRepository,
    private val session: SessionManager,
    private val accountRepository: AccountRepository
): TransactionFeature{
    companion object {
        private val logger = Logger.getLogger(TransactionFeatureImpl::class.java.name)
    }

    private fun updateSheetSoldFrom(account: Account, month: Month){
        val sheets = account.transactionsByMonthSortedByDate(month)
        for((position, number) in sheets.indices.withIndex()) {
            sheets[number].position = position
        }
        transactionRepository.saveAllSheets(
            sheets.toList()
        )
    }
    private fun updateSheetPosition(account: Account, transaction: Transaction) {
        val sheets = account.transactionsFilterAndSortedByPositionBefore(transaction.position)
        for(number in sheets.indices) {
            val actualTransaction = sheets[number]
            if(actualTransaction.date == transaction.date) {
                continue
            }
            actualTransaction.position += 1
            if(actualTransaction.position <= transaction.position) {
                continue
            }
        }
        transactionRepository.saveAllSheets(sheets)
    }
    private fun updateSheetPositionFromPositionRange(account: Account, transaction: Transaction, range: IntRange) {
        val sheets = account.transactions.filter { range.first <= it.position }.sortedBy { it.position }
        for(number in range) {
            val actualTransaction = sheets[number]
            if(actualTransaction.date == transaction.date && actualTransaction.id != transaction.id) {
                continue
            }
            actualTransaction.position += 1
            if(actualTransaction.position <= transaction.position) {
                continue
            }
        }
        transactionRepository.saveAllSheets(sheets)
    }
    override fun editTransaction(
        userID: Long,
        accountID: Long,
        transaction: Transaction,
        token: UUID
    ): Response<Transaction> = session.authenticate(UserId(userID), token, roleUser){
        if(transaction.id == null) return@authenticate Response.invalid("L'ID de la transaction est null")

        val acc = accountRepository.findAccountByIdWithTransactions(accountID)
        val sheetFromResource = acc?.transactions?.find { it.id == transaction.id } ?: return@authenticate Response.notFound("Aucune transaction n'existe avec l'ID suivant : ${transaction.id}")

        if(sheetFromResource.amount != transaction.amount) {
            acc.updateSoldFromTransactions(sheetFromResource, transaction)
        }
        sheetFromResource.updateFromOther(transaction)
        sheetFromResource.lastModified = LocalDateTime.now()
        transactionRepository.save(sheetFromResource)

        return@authenticate accountRepository.editFromAnother(acc).run {
            this ?: return@authenticate Response.invalid("Une erreur est survenu lors de la sauvegarde de la transaction")
            Response.ok(sheetFromResource)
        }
    }
    override fun bookTransaction(
        userId: UserId,
        token: UUID,
        accountLabel: String,
        transaction: Transaction
    ): Response<Transaction> = session.authenticate(userId, token) {
        val account = accountRepository.findAccountByLabelWithTransactions(userId, accountLabel) ?: return@authenticate Response.notFound("Le compte $accountLabel n'existe pas")
        account.addTransaction(transaction)
        accountRepository.upsert(account)
        Response.ok(transaction)
    }

    override fun retrieveTransactionsByMonthAndYear(
        userId: UserId,
        token: UUID,
        month: Month,
        year: Int,
        account: String
    ): Response<List<Transaction>> = session.authenticate(userId, token) {
        val user = userRepository.findUserById(userId) ?: return@authenticate Response.notFound("L'utilisateur n'existe pas")
        Response.ok(transactionRepository.findAccountWithSheetByLabelAndUser(account, user.id)
            ?.retrieveSheetSurroundAndSortedByDate(month, year)
            ?: return@authenticate Response.notFound("Aucun compte ne correspond au label indiqué")
        )
    }

    override fun findById(
        userID: Long,
        id: Long,
        token: UUID
    ): Response<Transaction> = session.authenticate(UserId(userID), token, roleUser) {
        val sheet = transactionRepository.findTransactionById(id) ?: return@authenticate Response.notFound("La transaction n'existe pas")
        Response.ok(sheet)
    }

    override fun deleteSheetsByIds(userId: UserId, accountID: Long, sheetIds: List<Long>, token: UUID) {
        val account: Account = accountRepository.findAccountByIdWithTransactions(accountID) ?: return
        val isSheetOnList: (s: Transaction) -> Boolean = { sheetIds.contains(it.id) }
        val month = account.transactions.filter(isSheetOnList)[0].date.month
        account.cancelSheetsAmount(account.transactions.filter(isSheetOnList))
        account.transactions.removeIf(isSheetOnList)
        updateSheetSoldFrom(account, month)
        accountRepository.upsert(account)
        transactionRepository.deleteAllSheetsById(sheetIds)
    }

}
