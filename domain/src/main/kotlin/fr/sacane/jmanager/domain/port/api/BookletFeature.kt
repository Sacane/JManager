package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.port.spi.*
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.logging.Logger

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
    private val accountRepository: BookletRepositoryPort,
    private val regularTransactionRepository: RegularTransactionRepository,
    private val regularTransactionGeneratorService: RegularTransactionGenerator,
    private val unitOfWorkTransactionProviderPort: UnitOfWorkTransactionProviderPort,
    private val trackerRepository: RegularTransactionTrackerRepository
): BookletFeature {
    companion object {
        private val LOGGER = Logger.getLogger(BookletFeatureImpl::class.java.name)
    }
    override fun findAccountById(
        accountID: Long,
        token: String
    ): Result<Booklet> = session.authenticate(token) {
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
        return@authenticate unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {
            if(accountRepository.findAccountByIdWithTransactions(accountID) == null){
                return@executeInTransaction failure(ResultState.NOT_FOUND, "Le livret $accountID n'existe pas")
            }
            accountRepository.deleteAccountById(accountID)
            trackerRepository.deleteTrackerByBookletId(accountID)
            return@executeInTransaction success()
        }
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
            LOGGER.info("Loading transactions for booklet $bookletId for month $month and year $year")
            val regularTransactions = regularTransactionRepository.getAllRegularUsedByAccount(userId, bookletId)
                ?: return@executeInTransaction failure(ResultState.BOOKLET_NOT_FOUND, "Regular transactions not found for this account")

            val booklet: Booklet = accountRepository.findAccountByIdWithTransactions(bookletId)
                ?: return@executeInTransaction failure(ResultState.BOOKLET_NOT_FOUND, "Requested booklet is not registered")

            val currentDate = LocalDate.now()
            val currentMonth = currentDate.month
            val currentYear = currentDate.year
            
            val monthsToGenerate = generateMonthRange(currentMonth, currentYear, month, year)
            val allGeneratedTransactions = mutableListOf<Transaction>()
            
            monthsToGenerate.forEach { (genMonth, genYear) ->
                val generated = regularTransactionGeneratorService.generateMissingPrevisionalTransactions(
                    bookletId,
                    regularTransactions,
                    genMonth,
                    genYear
                )
                allGeneratedTransactions.addAll(generated)
            }
            
            LOGGER.info("Generated ${allGeneratedTransactions.size} transactions for months from $currentMonth/$currentYear to $month/$year")

            if (allGeneratedTransactions.isNotEmpty()) {
                allGeneratedTransactions.forEach {
                    booklet.addTransaction(it)
                }
                accountRepository.update(booklet)
            }

            val transactions = booklet.retrieveSheetSurroundAndSortedByDate(month, year).partition { it.isPreview }

            val previsionalSold = calculatePrevisionalSold(
                booklet,
                currentMonth,
                currentYear,
                month,
                year
            )

            val bookletLoadingResult = BookletLoadingResult(
                label = booklet.label,
                currentTransactions = transactions.second,
                previsionalTransactions = transactions.first,
                regularTransactions = regularTransactions,
                realSold = booklet.amount,
                previsionalSold = previsionalSold
            )
            LOGGER.info("""
                Booklet loaded successfully :
                Label: ${booklet.label}
                Current transactions: ${transactions.second.size}
                Previsional transactions: ${transactions.first.size}
                Regular transactions: ${regularTransactions.size}
                Real sold: ${booklet.amount}
                Previsional sold: $previsionalSold
            """.trimIndent())
            return@executeInTransaction success(bookletLoadingResult)
        }
    }

    /**
     * Generates a list of months between the start month and year and the end month and year.
     */
    private fun generateMonthRange(
        startMonth: Month,
        startYear: Int,
        endMonth: Month,
        endYear: Int
    ): List<Pair<Month, Int>> {
        val months = mutableListOf<Pair<Month, Int>>()
        var currentMonth = startMonth
        var currentYear = startYear
        
        while (currentYear < endYear || (currentYear == endYear && currentMonth <= endMonth)) {
            months.add(Pair(currentMonth, currentYear))
            
            if (currentMonth == Month.DECEMBER) {
                currentMonth = Month.JANUARY
                currentYear++
            } else {
                currentMonth = Month.of(currentMonth.value + 1)
            }
        }
        
        return months
    }

    /**
     * Calculates the provisional balance (sold) for a booklet between the current month and year
     * and a target month and year, considering all relevant transactions in the specified date range.
     *
     * @param booklet the booklet object containing the current balance, transactions, and other details
     * @param currentMonth the current month used as the starting point of the calculation
     * @param currentYear the current year used as the starting point of the calculation
     * @param targetMonth the target month up to which the balance is calculated
     * @param targetYear the target year up to which the balance is calculated
     * @return the provisional balance as an `Amount` object
     */
    private fun calculatePrevisionalSold(
        booklet: Booklet,
        currentMonth: Month,
        currentYear: Int,
        targetMonth: Month,
        targetYear: Int
    ): Amount {
        val currentDate = LocalDate.now()
        val allTransactions = booklet.transactions
        
        val relevantTransactions = allTransactions.filter { transaction ->
            val transactionDate = transaction.date
            val transactionYearMonth = transactionDate.year * 12 + transactionDate.monthValue
            val currentYearMonth = currentYear * 12 + currentMonth.value
            val targetYearMonth = targetYear * 12 + targetMonth.value

            when {
                transactionYearMonth < currentYearMonth -> false
                transactionYearMonth == currentYearMonth -> {
                    !transaction.isPreview || transactionDate.isAfter(currentDate) || transactionDate.isEqual(currentDate)
                }
                transactionYearMonth <= targetYearMonth -> true
                else -> false
            }
        }
        
        val totalAmount = relevantTransactions.fold(BigDecimal.ZERO) { acc, transaction ->
            val value = transaction.amount.value.abs()
            if (transaction.isIncome) {
                acc.add(value)
            } else {
                acc.subtract(value)
            }
        }
        
        return Amount(booklet.amount.value.add(totalAmount))
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