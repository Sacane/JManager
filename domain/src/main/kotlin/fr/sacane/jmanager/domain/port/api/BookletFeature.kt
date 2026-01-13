package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.port.spi.*
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import java.util.logging.Logger

@Port(Side.APPLICATION)
/**
 * Application port: BookletFeature
 *
 * High-level API for managing booklets (accounts) exposed to the application layer.
 * Implementations are responsible for authentication and returning domain Result<T>
 * signalling success or failure states.
 */
sealed interface BookletFeature {
    /**
     * Find a booklet (account) by its unique identifier.
     *
     * @param accountID The UUID of the booklet to find.
     * @param token Authentication token identifying the requester.
     * @return Result containing the found Booklet on success, or a failure state (e.g. BOOKLET_NOT_FOUND).
     */
    fun findAccountById(accountID: UUID, token: String): Result<Booklet>

    /**
     * Edit an existing booklet.
     *
     * @param booklet The Booklet object containing updated values (must include an id).
     * @param token Authentication token identifying the requester.
     * @return Result containing the updated Booklet on success, or failure states when validation or persistence fails.
     */
    fun editAccount(booklet: Booklet, token: String): Result<Booklet>

    /**
     * Delete a booklet by its identifier.
     *
     * @param accountID The UUID of the booklet to delete.
     * @param token Authentication token identifying the requester.
     * @return Result with no value on success, or an error state if the booklet does not exist.
     */
    fun deleteAccountById(accountID: UUID, token: String): Result<Nothing>

    /**
     * Find a booklet by its label for the authenticated user.
     *
     * @param token Authentication token identifying the requester.
     * @param label Label of the booklet to find.
     * @return Result containing the Booklet on success, or BOOKLET_LABEL_NOT_EXIST when not found.
     */
    fun findByLabelAndUserId(token: String, label: String): Result<Booklet>

    /**
     * Retrieve all booklets registered for the authenticated user.
     *
     * @param token Authentication token identifying the requester.
     * @return Result containing the list of Booklet on success.
     */
    fun findAllRegisteredAccounts(token: String): Result<List<Booklet>>

    /**
     * Save a new booklet for the authenticated user.
     *
     * @param token Authentication token identifying the requester.
     * @param booklet Booklet object to save.
     * @return Result containing the saved Booklet on success, or a failure when the label already exists or persistence fails.
     */
    fun save(token: String, booklet: Booklet): Result<Booklet>

    /**
     * Load transactions for a specific booklet for a given month and year.
     * This may generate provisional (preview) transactions for missing regular transactions
     * and compute provisional balances.
     *
     * @param token Authentication token identifying the requester.
     * @param bookletId UUID of the booklet to load.
     * @param month Target month to load transactions for.
     * @param year Target year to load transactions for.
     * @param startingMonth Starting month for calculation (defaults to current month if null).
     * @param startingYear Starting year for calculation (defaults to current year if null).
     * @return Result containing a BookletLoadingResult on success, or a failure state when the booklet is not found.
     */
    fun loadTransactionsForBookletForAMonth(
        token: String,
        bookletId: UUID,
        month: Month,
        year: Int,
        startingMonth: Month? = null,
        startingYear: Int? = null
    ): Result<BookletLoadingResult>
}

@DomainService
class BookletFeatureImpl(
    private val userRepository: UserRepository,
    private val session: SessionManager,
    private val accountRepository: BookletRepository,
    private val regularTransactionRepository: RegularTransactionRepository,
    private val regularTransactionGeneratorService: RegularTransactionGenerator,
    private val unitOfWorkTransactionProviderPort: UnitOfWorkTransactionProvider,
    private val trackerRepository: RegularTransactionTrackerRepository
): BookletFeature {
    companion object {
        private val LOGGER = Logger.getLogger(BookletFeatureImpl::class.java.name)
    }
    override fun findAccountById(
        accountID: UUID,
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
        accountID: UUID,
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
        if (user.booklets.size >= 6) {
            return@authenticate failure(ResultState.BOOKLET_MAXIMUM_SIZE_REACHED, "Le profil ne peut pas contenir plus de 6 comptes")
        }
        val accountSaved = accountRepository.save(it, booklet)
            ?: return@authenticate failure(ResultState.INFRASTRUCTURE_ERROR,"Erreur lors de la sauvegarde du compte")
        success(accountSaved)
    }

    override fun loadTransactionsForBookletForAMonth(
        token: String,
        bookletId: UUID,
        month: Month,
        year: Int,
        startingMonth: Month?,
        startingYear: Int?
    ): Result<BookletLoadingResult> = session.authenticate(token) { userId ->
        return@authenticate unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {
            LOGGER.info("Loading transactions for booklet $bookletId for month $month and year $year")

            val booklet: Booklet = accountRepository.findAccountByIdWithTransactions(bookletId)
                ?: return@executeInTransaction failure(ResultState.BOOKLET_NOT_FOUND, "Requested booklet is not registered")

            val regularTransactions = regularTransactionRepository.getAllRegularUsedByAccount(userId, bookletId)
                ?: emptyList()

            val currentDate = LocalDate.now()
            val currentMonth = startingMonth ?: currentDate.month
            val currentYear = startingYear ?: currentDate.year

            // Only generate physical previsional transactions for the CURRENT month
            // For other months, we'll use virtual transactions in the calculation
            val targetYearMonth = YearMonth.of(year, month)
            val currentYearMonth = YearMonth.of(currentYear, currentMonth)

            val generatedTransactions = if (targetYearMonth.equals(currentYearMonth)) {
                // Generate missing previsional transactions for current month only
                // This checks for existing transactions and only creates what's missing
                val transactions = regularTransactionGeneratorService.generateMissingPrevisionalTransactions(
                    bookletId,
                    regularTransactions,
                    month,
                    year
                )
                LOGGER.info("Generated ${transactions.size} physical transactions for current month $month/$year")
                transactions
            } else {
                LOGGER.info("Skipping physical transaction generation for non-current month $month/$year")
                emptyList()
            }

            if (generatedTransactions.isNotEmpty()) {
                generatedTransactions.forEach {
                    booklet.addTransaction(it)
                }
                accountRepository.update(booklet)
            }

            val transactions = booklet.retrieveSheetSurroundAndSortedByDate(month, year).partition { it.isPreview }

            val previsionalSold = calculatePrevisionalSold(
                booklet,
                regularTransactions,
                currentMonth,
                currentYear,
                month,
                year
            )

            val requestedDate = LocalDate.of(year, month, 1)
            val filteredRegularTransactions = regularTransactions.filter { rt ->
                !rt.startDate.isAfter(requestedDate.withDayOfMonth(requestedDate.lengthOfMonth()))
            }

            val bookletLoadingResult = BookletLoadingResult(
                label = booklet.label,
                currentTransactions = transactions.second,
                previsionalTransactions = transactions.first,
                regularTransactions = filteredRegularTransactions,
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
     * Calculates the provisional balance (sold) for a booklet between the current month and year
     * and a target month and year, considering all relevant transactions in the specified date range.
     *
     * @param booklet the booklet object containing the current balance, transactions, and other details
     * @param regularTransactions the list of regular transactions to include as virtual transactions
     * @param currentMonth the current month used as the starting point of the calculation
     * @param currentYear the current year used as the starting point of the calculation
     * @param targetMonth the target month up to which the balance is calculated
     * @param targetYear the target year up to which the balance is calculated
     * @return the provisional balance as an `Amount` object
     */
    private fun calculatePrevisionalSold(
        booklet: Booklet,
        regularTransactions: List<RegularTransaction>,
        currentMonth: Month,
        currentYear: Int,
        targetMonth: Month,
        targetYear: Int
    ): Amount {
        val allTransactions = booklet.transactions

        // Get only PREVIEW (previsional) physical transactions in the date range
        // Real transactions are already counted in booklet.amount, so we must not count them again
        val relevantPreviewTransactions = allTransactions.filter { transaction ->
            if (!transaction.isPreview) return@filter false

            val transactionDate = transaction.date
            val transactionYearMonth = transactionDate.year * 12 + transactionDate.monthValue
            val currentYearMonth = currentYear * 12 + currentMonth.value
            val targetYearMonth = targetYear * 12 + targetMonth.value

            when {
                transactionYearMonth < currentYearMonth -> false
                transactionYearMonth <= targetYearMonth -> true
                else -> false
            }
        }

        // Calculate virtual transactions from regular transactions for the date range
        // These are transactions that would be generated but haven't been physically created yet
        val virtualTransactions = regularTransactionGeneratorService.calculateVirtualTransactions(
            regularTransactions,
            currentMonth,
            currentYear,
            targetMonth,
            targetYear
        )

        // Filter out virtual transactions that already exist as physical preview transactions
        // to avoid double-counting. We check both the regularTransactionId and date match
        // to properly identify existing preview transactions
        val existingPreviewKeys = relevantPreviewTransactions
            .filter { it.regularTransactionId != null }
            .map { "${it.regularTransactionId}-${it.date}" }
            .toSet()

        val nonDuplicateVirtualTransactions = virtualTransactions.filter { vt ->
            "${vt.regularTransactionId}-${vt.date}" !in existingPreviewKeys
        }

        // Combine preview physical transactions and virtual transactions for calculation
        // Real transactions are already in booklet.amount
        val allRelevantTransactions = relevantPreviewTransactions + nonDuplicateVirtualTransactions

        val totalAmount = allRelevantTransactions.fold(BigDecimal.ZERO) { acc, transaction ->
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