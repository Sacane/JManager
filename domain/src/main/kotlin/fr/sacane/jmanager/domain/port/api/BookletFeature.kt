package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.port.spi.AccountRepositoryPort
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.TransactionRepositoryPort
import fr.sacane.jmanager.domain.port.spi.UnitOfWorkTransactionProviderPort
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import fr.sacane.jmanager.domain.usecase.RegularTransactionGeneratorService
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

@Port(Side.APPLICATION)
sealed interface BookletFeature {
    fun findAccountById(accountID: Long, token: String): Result<Booklet>
    fun editAccount(booklet: Booklet, token: String): Result<Booklet>
    fun deleteAccountById(accountID: Long, token: String): Result<Nothing>
    fun findByLabelAndUserId(token: String, label: String): Result<Booklet>
    fun findAllRegisteredAccounts(token: String): Result<List<Booklet>>
    fun save(token: String, booklet: Booklet): Result<Booklet>
    fun loadTransactionsForBookletForAMonth(token: String, bookletId: Long, month: Month, year: Int): Result<BookletLoadingResult>
}

@DomainService
class BookletFeatureImpl(
    private val userRepository: UserRepository,
    private val session: SessionManager,
    private val accountRepository: AccountRepositoryPort,
    private val regularTransactionRepository: RegularTransactionRepository,
    private val regularTransactionGeneratorService: RegularTransactionGenerator,
    private val unitOfWorkTransactionProviderPort: UnitOfWorkTransactionProviderPort
): BookletFeature {
    override fun findAccountById(
        accountID: Long,
        token: String
    ): Result<Booklet> = session.authenticate(token) {
        val regularTransactions = regularTransactionRepository.getAllRegularUsedByAccount(it, accountID)
        accountRepository.findAccountByIdWithTransactions(accountID)?.run {
            success(this)
        } ?: failure(ResultState.BOOKLET_NOT_FOUND, "Le compte est introuvable")
    }

    override fun editAccount(
        booklet: Booklet,
        token: String
    ): Result<Booklet> = session.authenticate(token) {
        val accountID = booklet.id ?: return@authenticate failure(ResultState.BOOKLET_NOT_FOUND, "Le livret ${booklet.label} est introuvable en base")
        val oldAccount = accountRepository.findAccountByIdWithTransactions(accountID) ?: return@authenticate failure(ResultState.BOOKLET_NOT_FOUND, "Le livret ${booklet.id} est introuvable")
        if(oldAccount.id != booklet.id && oldAccount.label == booklet.label){
            return@authenticate failure(ResultState.BOOKLET_LABEL_EXIST, "Le libellé du livret existe déjà")
        }
        oldAccount.updateFrom(booklet)
        val registered = accountRepository.upsert(oldAccount)
        success(registered)
    }

    override fun deleteAccountById(
        accountID: Long,
        token: String
    ): Result<Nothing> = session.authenticate(token) {
        if(accountRepository.findAccountByIdWithTransactions(accountID) == null){
            return@authenticate failure(ResultState.NOT_FOUND, "Le livret $accountID n'existe pas")
        }
        accountRepository.deleteAccountById(accountID)
        success()
    }

    override fun findByLabelAndUserId(
        token: String,
        label: String
    ): Result<Booklet> = session.authenticate(token) {
        val user = userRepository.findUserByIdWithAccounts(it)
            ?: return@authenticate failure(ResultState.USER_NOT_FOUND, "L'utilisateur recherché n'existe pas")
        success(
            user.booklets
            .find { acc -> acc.label == label }
            ?: return@authenticate failure(ResultState.BOOKLET_LABEL_NOT_EXIST, "Le compte $label n'est pas enregistré en base")
        )
    }

    override fun findAllRegisteredAccounts(
        token: String
    ): Result<List<Booklet>> = session.authenticate(token) {
        val user = userRepository.findUserByIdWithAccounts(it)
            ?: return@authenticate failure(ResultState.BOOKLET_NOT_FOUND, "L'utilisateur n'existe pas en base")
        return@authenticate success(user.booklets)
    }

    override fun save(
        token: String,
        booklet: Booklet
    ): Result<Booklet> = session.authenticate(token) {
        val user = userRepository.findUserByIdWithAccounts(it)
            ?: return@authenticate failure(ResultState.USER_NOT_FOUND, "L'utilisateur n'existe pas en base")
        if(user.hasAccount(booklet.label)) {
            return@authenticate failure(ResultState.BOOKLET_LABEL_EXIST, "Le profil contient déjà un compte avec le label ${booklet.label}")
        }
        val accountSaved = accountRepository.save(it, booklet)
            ?: return@authenticate failure(ResultState.INFRASTRUCTURE_ERROR,"Erreur lors de la sauvegarde du compte")
        success(accountSaved)
    }

    override fun loadTransactionsForBookletForAMonth(
        token: String,
        bookletId: Long,
        month: Month,
        year: Int
    ): Result<BookletLoadingResult> = session.authenticate(token) { userId ->
        return@authenticate unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {

            val regularTransactions = regularTransactionRepository.getAllRegularUsedByAccount(userId, bookletId)
                ?: return@executeInTransaction failure(ResultState.BOOKLET_NOT_FOUND, "Regular transactions not found for this account")

            regularTransactionGeneratorService.generateMissingPrevisionalTransactions(
                bookletId,
                regularTransactions,
                month,
                year
            )
            val booklet: Booklet = accountRepository.findAccountByIdWithTransactions(bookletId)
                ?: return@executeInTransaction failure(ResultState.BOOKLET_NOT_FOUND, "Requested booklet is not registered")

            val transactions = booklet.retrieveSheetSurroundAndSortedByDate(month, year).partition { it.isPreview }

            return@executeInTransaction success(BookletLoadingResult(
                label = booklet.label,
                currentTransactions = transactions.second,
                previsionalTransactions = transactions.first,
                regularTransactions = regularTransactions,
                realSold = booklet.amount,
                previsionalSold = booklet.previewAmount
            ))
        }
    }
}

data class BookletLoadingResult(
    val label: String,
    val currentTransactions: List<Transaction>,
    val previsionalTransactions: List<Transaction>,
    val regularTransactions: List<RegularTransaction>,
    val realSold: Amount,
    val previsionalSold: Amount
)