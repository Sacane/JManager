package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.AccountRepositoryPort
import fr.sacane.jmanager.domain.port.spi.InfraTransactionProviderPort
import fr.sacane.jmanager.domain.port.spi.TransactionRepositoryPort
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.time.LocalDateTime
import java.time.Month
import java.util.*
import java.util.logging.Logger

@Port(Side.APPLICATION)
sealed interface TransactionFeature {
    fun bookTransaction(userId: UserId, token: UUID, accountLabel: String, transaction: Transaction): Response<TransactionCreationResult>
    fun retrieveTransactionsByMonthAndYear(userId: UserId, token: UUID, month: Month, year: Int, account: String): Response<List<Transaction>>
    fun editTransaction(userID: Long, accountID: Long, transaction: Transaction, token: UUID): Response<TransactionCreationResult>
    fun findById(userID: Long, id: Long, token: UUID): Response<Transaction>
    fun deleteSheetsByIds(userId: UserId, accountID: Long, sheetIds: List<Long>, token: UUID)
}

@DomainService
class TransactionFeatureImpl(
    private val transactionRepository: TransactionRepositoryPort,
    private val userRepository: UserRepository,
    private val session: SessionManager,
    private val accountRepository: AccountRepositoryPort,
    private val infraTransactionManager: InfraTransactionProviderPort
): TransactionFeature{
    companion object {
        private val logger = Logger.getLogger(TransactionFeatureImpl::class.java.name)
    }

    override fun editTransaction(
        userID: Long,
        accountID: Long,
        transaction: Transaction,
        token: UUID
    ): Response<TransactionCreationResult> = session.authenticate(UserId(userID), token, roleUser){
        return@authenticate infraTransactionManager.executeInTransaction(transaction) {
            if(transaction.id == null) return@executeInTransaction Response.invalid("L'ID de la transaction est null")

            val acc = accountRepository.findAccountByIdWithTransactions(accountID) ?: return@executeInTransaction Response.notFound()
            val transactionFromDatabase = transactionRepository.findTransactionById(transaction.id) ?: return@executeInTransaction Response.notFound("Aucune transaction n'existe avec l'ID suivant : ${transaction.id}")
            transactionFromDatabase.updateFromOther(transaction)
            transaction.lastModified = LocalDateTime.now()
            transactionRepository.save(acc.id!!, transaction) ?: return@executeInTransaction Response.invalid("Une erreur est survenue lors de la mise à jour de la transaction ${transactionFromDatabase.id}")
            acc.removeTransactionById(transaction.id)
            acc.addTransaction(transaction)
            accountRepository.upsert(acc)
            Response.ok(TransactionCreationResult(transaction, acc.amount, acc.previewAmount))
        }
    }

    override fun bookTransaction(
        userId: UserId,
        token: UUID,
        accountLabel: String,
        transaction: Transaction
    ): Response<TransactionCreationResult> = session.authenticate(userId, token) {
        return@authenticate infraTransactionManager.executeInTransaction(transaction) {
            val account = accountRepository.findAccountByLabelWithTransactions(userId, accountLabel) ?: return@executeInTransaction Response.notFound("Le compte $accountLabel n'existe pas")
            val newTr =  transactionRepository.save(account.id!!, transaction) ?: return@executeInTransaction Response.invalid("Erreur est survenu lors de la transaction")
            account.addTransaction(newTr)
            accountRepository.update(account)
            Response.ok(TransactionCreationResult(newTr, account.amount, account.previewAmount))
        }
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
        infraTransactionManager.executeInTransaction(transactionRepository) {
            val account: Account = accountRepository.findAccountByIdWithTransactions(accountID) ?: return@executeInTransaction
            val isSheetOnList: (s: Transaction) -> Boolean = { sheetIds.contains(it.id) }
            account.removeTransactionIf(isSheetOnList)
            accountRepository.upsert(account)
            transactionRepository.deleteAllSheetsById(sheetIds)
        }
    }

}
