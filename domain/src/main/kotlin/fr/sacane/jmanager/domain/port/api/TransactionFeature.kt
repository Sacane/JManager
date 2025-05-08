package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.AccountRepositoryPort
import fr.sacane.jmanager.domain.port.spi.InfraTransactionProviderPort
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.TransactionRepositoryPort
import fr.sacane.jmanager.domain.utils.*
import java.time.LocalDateTime
import java.time.Month
import java.util.logging.Logger

@Port(Side.APPLICATION)
sealed interface TransactionFeature {
    fun bookTransaction(userId: UserId, token: String, accountLabel: String, transaction: Transaction): Result<TransactionResumeResult>
    fun retrieveTransactionsByMonthAndYear(userId: UserId, token: String, month: Month, year: Int, account: String): Result<List<Transaction>>
    fun editTransaction(userID: Long, accountID: Long, transaction: Transaction, token: String): Result<TransactionResumeResult>
    fun findById(userID: Long, id: Long, token: String): Result<Transaction>
    fun deleteSheetsByIds(userId: UserId, accountID: Long, sheetIds: List<Long>, token: String): Result<Nothing>
    fun confirmPreviewTransaction(userId: UserId, token: String, accountID: Long, transactionId: Long): Result<TransactionResumeResult>
}

@DomainService
class TransactionFeatureImpl(
    private val transactionRepository: TransactionRepositoryPort,
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
        token: String
    ): Result<TransactionResumeResult> = session.authenticate(token, roleUser){
        return@authenticate infraTransactionManager.executeInTransaction(transaction) {
            if(transaction.id == null) return@executeInTransaction failure(ResultState.TRANSACTION_ENTRY_ERROR, "L'ID de la transaction est null")
            val registeredAccount = accountRepository.findAccountByIdWithTransactions(accountID) ?: return@executeInTransaction notFound("Le compte $accountID n'existe pas")
            val transactionFromDatabase = registeredAccount.findTransactionById(transaction.id)?.copy() ?: return@executeInTransaction notFound("Aucune transaction n'existe avec l'ID suivant : ${transaction.id}")
            transactionFromDatabase.updateFromOther(transaction)
            transaction.lastModified = LocalDateTime.now()
            transactionRepository.save(registeredAccount.id!!, transaction) ?: return@executeInTransaction invalid("Une erreur est survenue lors de la mise à jour de la transaction ${transactionFromDatabase.id}")
            registeredAccount.removeTransactionById(transaction.id)
            registeredAccount.addTransaction(transaction)
            accountRepository.update(registeredAccount)
            success(TransactionResumeResult(transaction, registeredAccount.amount, registeredAccount.previewAmount))
        }
    }

    override fun bookTransaction(
        userId: UserId,
        token: String,
        accountLabel: String,
        transaction: Transaction
    ): Result<TransactionResumeResult> = session.authenticate(token) {
        return@authenticate infraTransactionManager.executeInTransaction(transaction) {
            val account = accountRepository.findAccountByLabelWithTransactions(userId, accountLabel)
                ?: return@executeInTransaction failure(ResultState.TRANSACTION_NOT_FOUND, "Le compte $accountLabel n'existe pas")
            val newTr =  transactionRepository.save(account.id!!, transaction)
                ?: return@executeInTransaction failure(ResultState.INFRASTRUCTURE_ERROR, "Erreur est survenu lors de la transaction")
            if(transaction.amount.isNegative()) {
                return@executeInTransaction failure(ResultState.TRANSACTION_ENTRY_ERROR, "Le montant de la transaction ne peut pas être négatif")
            }
            account.addTransaction(newTr)
            accountRepository.update(account)
            success(TransactionResumeResult(newTr, account.amount, account.previewAmount))
        }
    }

    override fun retrieveTransactionsByMonthAndYear(
        userId: UserId,
        token: String,
        month: Month,
        year: Int,
        account: String
    ): Result<List<Transaction>> = session.authenticate(token) {
        success(transactionRepository.findAccountWithSheetByLabelAndUser(account, userId)?.retrieveSheetSurroundAndSortedByDate(month, year)
            ?: return@authenticate notFound("Aucun compte ne correspond au label indiqué")
        )
    }

    override fun findById(
        userID: Long,
        id: Long,
        token: String
    ): Result<Transaction> = session.authenticate(token, roleUser) {
        logger.info("Request for a transaction with id $id")
        val sheet = transactionRepository.findTransactionById(id) ?: return@authenticate failure(ResultState.TRANSACTION_NOT_FOUND, "La transaction $id n'existe pas")
        success(sheet)
    }

    override fun deleteSheetsByIds(userId: UserId, accountID: Long, sheetIds: List<Long>, token: String): Result<Nothing> {
        return infraTransactionManager.executeInTransaction(transactionRepository) {
            val account: Account = accountRepository.findAccountByIdWithTransactions(accountID) ?: return@executeInTransaction failure<Nothing>(ResultState.BOOKLET_NOT_FOUND, "Account $accountID n'existe pas")
            val isSheetOnList: (s: Transaction) -> Boolean = { sheetIds.contains(it.id) }
            account.removeTransactionIf(isSheetOnList)
            accountRepository.upsert(account)
            transactionRepository.deleteAllSheetsById(sheetIds)
            return@executeInTransaction success()
        }
    }

    override fun confirmPreviewTransaction(
        userId: UserId,
        token: String,
        accountID: Long,
        transactionId: Long
    ): Result<TransactionResumeResult> = session.authenticate(token) {
        return@authenticate infraTransactionManager.executeInTransaction(Any()) {
            val account = accountRepository.findAccountByIdWithTransactions(accountID)
                ?: return@executeInTransaction failure(ResultState.BOOKLET_NOT_FOUND, "Booklet $accountID not found")
            val transaction = transactionRepository.findTransactionById(transactionId)
                ?: return@executeInTransaction failure(ResultState.BOOKLET_NOT_FOUND, "Transaction not found")
            transaction.isPreview = false
            account.removeTransactionById(transactionId)
            account.addTransaction(transaction)
            accountRepository.upsert(account)
            return@executeInTransaction success(TransactionResumeResult(transaction, account.amount, account.previewAmount))
        }
    }

}
