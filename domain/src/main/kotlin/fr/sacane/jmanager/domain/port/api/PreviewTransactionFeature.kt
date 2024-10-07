package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.AccountRepositoryPort
import fr.sacane.jmanager.domain.port.spi.TransactionRepositoryPort
import java.time.LocalDate
import java.time.Month

@Port(Side.APPLICATION)
interface PreviewTransactionFeature {
    fun bookPreviewTransaction(
        userAccountID: UserAccountID,
        label: String,
        date: LocalDate,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag = Tag("Aucune", isDefault = true),
        transactionId: Long? = null
    ): Response<Transaction>

    fun findPreviewTransactionWithYearAndMonth(userAccountID: UserAccountID, month: Month, year: Int): Response<List<Transaction>>
}

@DomainService
class PreviewTransactionFeatureImpl(
    private val accountRepository: AccountRepositoryPort,
    private val transactionRepositoryPort: TransactionRepositoryPort,
    private val sessionManager: SessionManager
) : PreviewTransactionFeature {
    override fun bookPreviewTransaction(
        userAccountID: UserAccountID,
        label: String,
        date: LocalDate,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag,
        transactionId: Long?
    ): Response<Transaction> = sessionManager.authenticate(userAccountID.userId, userAccountID.token.tokenValue) {
        val account = accountRepository.findAccountByIdWithTransactions(accountId = userAccountID.accountID) ?: return@authenticate Response.notFound<Transaction>("Le compte ${userAccountID.accountID} n'existe pas")
        val transaction = Transaction(transactionId, label, date, amount, isIncome, isPreview = true)
        account.addTransaction(transaction)
        val savedTransaction = transactionRepositoryPort.save(transaction) ?: return@authenticate Response.invalid("Une erreur a été détecté lors de la création de la preview transaction ${transaction.label}")
        accountRepository.upsert(account)
        return@authenticate Response.ok(savedTransaction)
    }

    override fun findPreviewTransactionWithYearAndMonth(
        userAccountID: UserAccountID,
        month: Month,
        year: Int
    ): Response<List<Transaction>> = sessionManager.authenticate(userAccountID.userId, userAccountID.token.tokenValue) {
        val account = accountRepository.findAccountByIdWithTransactions(accountId = userAccountID.accountID) ?: return@authenticate Response.notFound<List<Transaction>>("Le compte ${userAccountID.accountID} n'existe pas")
        val result = account.retrieveSheetSurroundAndSortedByDate(month, year, true)
        return@authenticate Response.ok(result)
    }
}