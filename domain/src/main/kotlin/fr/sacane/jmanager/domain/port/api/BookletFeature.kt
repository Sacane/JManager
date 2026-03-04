package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.BookletBalances
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.port.spi.*
import fr.sacane.jmanager.domain.port.spi.repository.BookletBalanceQueryRepository
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionQueryRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.math.BigDecimal
import java.time.Duration
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
 * signaling success or failure states.
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

    /**
     * Load only the balances (soldes) for a specific booklet for a given month and year.
     * This uses the existing transaction loading logic but filters the result to return only
     * the balances information.
     *
     * @param token Authentication token identifying the requester.
     * @param bookletId UUID of the booklet to load balances for.
     * @param month Target month to load balances for.
     * @param year Target year to load balances for.
     * @param startingMonth Starting month for calculation (defaults to current month if null).
     * @param startingYear Starting year for calculation (defaults to current year if null).
     * @return Result containing the BookletBalances on success, or a failure state when the booklet is not found.
     */
    fun loadBalancesForBookletForAMonth(
        token: String,
        bookletId: UUID,
        month: Month,
        year: Int,
        startingMonth: Month? = null,
        startingYear: Int? = null
    ): Result<BookletBalances>
}

@DomainService
class BookletFeatureImpl(
    private val userRepository: UserRepository,
    private val session: SessionManager,
    private val accountRepository: BookletRepository,
    private val regularTransactionRepository: RegularTransactionRepository,
    private val regularTransactionGeneratorService: RegularTransactionGenerator,
    private val unitOfWorkTransactionProviderPort: UnitOfWorkTransactionProvider,
    private val trackerRepository: RegularTransactionTrackerRepository,
    private val transactionQueryRepository: TransactionQueryRepository,
    private val bookletBalanceQueryRepository: BookletBalanceQueryRepository
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
            val totalStartNs = System.nanoTime()
            LOGGER.info("Loading transactions for booklet $bookletId for month $month and year $year")

            val fetchBookletStartNs = System.nanoTime()
            val booklet: Booklet = accountRepository.findAccountByIdWithTransactions(bookletId)
                ?: return@executeInTransaction failure(ResultState.BOOKLET_NOT_FOUND, "Requested booklet is not registered")
            val fetchBookletMs = Duration.ofNanos(System.nanoTime() - fetchBookletStartNs).toMillis()

            val fetchRegularStartNs = System.nanoTime()
            val regularTransactions = regularTransactionRepository.getAllRegularUsedByAccount(userId, bookletId)
                ?: emptyList()
            val fetchRegularMs = Duration.ofNanos(System.nanoTime() - fetchRegularStartNs).toMillis()

            val currentDate = LocalDate.now()
            val currentMonth = startingMonth ?: currentDate.month
            val currentYear = startingYear ?: currentDate.year

            // Only generate physical previsional transactions for the CURRENT month
            // For other months, we'll use virtual transactions in the calculation
            val targetYearMonth = YearMonth.of(year, month)
            val currentYearMonth = YearMonth.of(currentYear, currentMonth)

            val generationStartNs = System.nanoTime()
            val generatedTransactions = if (targetYearMonth.equals(currentYearMonth)) {
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
            val generationMs = Duration.ofNanos(System.nanoTime() - generationStartNs).toMillis()

            val updateBookletStartNs = System.nanoTime()
            if (generatedTransactions.isNotEmpty()) {
                generatedTransactions.forEach { booklet.addTransaction(it) }
                accountRepository.update(booklet)
            }
            val updateBookletMs = Duration.ofNanos(System.nanoTime() - updateBookletStartNs).toMillis()

            // Read-optimized monthly fetch (DB side filtering/sorting)
            val monthSheetStartNs = System.nanoTime()
            val rangeStart = LocalDate.of(year, month, 1)
            val rangeEnd = rangeStart.withDayOfMonth(rangeStart.lengthOfMonth())
            val allTransactionsForMonth = transactionQueryRepository.findByBookletIdAndDateBetween(bookletId, rangeStart, rangeEnd)
            val monthSheetMs = Duration.ofNanos(System.nanoTime() - monthSheetStartNs).toMillis()

            // P0: avoid N+1 trackerRepository.findTracker(...) calls by preloading once
            val preloadTrackersStartNs = System.nanoTime()
            val trackersByRegularId = trackerRepository.findAllTrackersForBooklet(bookletId)
                .associateBy { it.regularTransactionId }
            val preloadTrackersMs = Duration.ofNanos(System.nanoTime() - preloadTrackersStartNs).toMillis()

            // Filter out preview transactions that fall in excluded months
            val filterExcludedStartNs = System.nanoTime()
            val filteredTransactions = allTransactionsForMonth.filter { transaction ->
                when {
                    !transaction.isPreview -> true
                    transaction.regularTransactionId == null -> true
                    else -> {
                        val tracker = trackersByRegularId[transaction.regularTransactionId]
                        val transactionYearMonth = YearMonth.from(transaction.date)
                        val isExcluded = tracker?.excludedMonths?.contains(transactionYearMonth) == true
                        !isExcluded
                    }
                }
            }
            val filterExcludedMs = Duration.ofNanos(System.nanoTime() - filterExcludedStartNs).toMillis()

            val transactions = filteredTransactions.partition { it.isPreview }

            val previsionalStartNs = System.nanoTime()
            val previsionalSold = calculatePrevisionalSold(
                booklet,
                regularTransactions,
                currentMonth,
                currentYear,
                month,
                year
            )
            val previsionalMs = Duration.ofNanos(System.nanoTime() - previsionalStartNs).toMillis()

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

            val totalMs = Duration.ofNanos(System.nanoTime() - totalStartNs).toMillis()
            LOGGER.info(
                """
                Booklet loaded successfully:
                - bookletId: $bookletId
                - period: $month/$year
                - sizes: monthTransactions=${allTransactionsForMonth.size}, current=${transactions.second.size}, preview=${transactions.first.size}, regular=${regularTransactions.size}, trackers=${trackersByRegularId.size}
                - timings(ms): fetchBooklet=$fetchBookletMs, fetchRegular=$fetchRegularMs, generate=$generationMs, updateBooklet=$updateBookletMs, monthQuery=$monthSheetMs, preloadTrackers=$preloadTrackersMs, filterExcluded=$filterExcludedMs, previsionalSold=$previsionalMs, total=$totalMs
                """.trimIndent()
            )

            return@executeInTransaction success(bookletLoadingResult)
        }
    }

    override fun loadBalancesForBookletForAMonth(
        token: String,
        bookletId: UUID,
        month: Month,
        year: Int,
        startingMonth: Month?,
        startingYear: Int?
    ): Result<BookletBalances> = session.authenticate(token) { userId ->
        return@authenticate unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {
            val persisted = bookletBalanceQueryRepository.findPersistedBalances(bookletId)
                ?: return@executeInTransaction failure(ResultState.BOOKLET_NOT_FOUND, "Requested booklet is not registered")

            val currentDate = LocalDate.now()
            val currentMonth = startingMonth ?: currentDate.month
            val currentYear = startingYear ?: currentDate.year

            val regularTransactions = regularTransactionRepository.getAllRegularUsedByAccount(userId, bookletId)
                ?: emptyList()

            // Only generate physical previsional transactions for the CURRENT month
            val targetYearMonth = YearMonth.of(year, month)
            val currentYearMonth = YearMonth.of(currentYear, currentMonth)
            if (targetYearMonth == currentYearMonth) {
                // side-effect: generator persists missing preview tx; doesn't require loading all sheets
                regularTransactionGeneratorService.generateMissingPrevisionalTransactions(
                    bookletId,
                    regularTransactions,
                    month,
                    year
                )
            }

            // Build a minimal booklet carrying only what calculatePrevisionalSold needs
            // (transactions are still needed for physical previews between current and target).
            val baseBooklet = Booklet(
                amount = Amount(persisted.amount),
                labelAccount = persisted.label,
                previewAmount = Amount(persisted.previewAmount),
                id = bookletId
            )

            val previewStart = YearMonth.of(currentYear, currentMonth)
            val previewEnd = YearMonth.of(year, month)
            val (from, to) = if (previewStart <= previewEnd) {
                val fromDate = LocalDate.of(currentYear, currentMonth, 1)
                val endDate = LocalDate.of(year, month, 1).withDayOfMonth(YearMonth.of(year, month).lengthOfMonth())
                fromDate to endDate
            } else {
                val fromDate = LocalDate.of(year, month, 1)
                val endDate = LocalDate.of(currentYear, currentMonth, 1).withDayOfMonth(YearMonth.of(currentYear, currentMonth).lengthOfMonth())
                fromDate to endDate
            }

            // Load physical transactions in the bounded range once:
            // - previews are needed for amount computation
            // - all physical transactions are needed for virtual deduplication
            val allPhysicalTransactionsInRange = transactionQueryRepository
                .findByBookletIdAndDateBetween(bookletId, from, to)

            val physicalPreviewTransactions = allPhysicalTransactionsInRange.filter { it.isPreview }

            physicalPreviewTransactions.forEach { baseBooklet.addTransaction(it) }

            val previsionalSold = calculatePrevisionalSold(
                baseBooklet,
                regularTransactions,
                currentMonth,
                currentYear,
                month,
                year,
                allPhysicalTransactionsForDedup = allPhysicalTransactionsInRange
            )

            return@executeInTransaction success(
                BookletBalances(
                    label = persisted.label,
                    realSold = Amount(persisted.amount),
                    previewSold = previsionalSold
                )
            )
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
        targetYear: Int,
        allPhysicalTransactionsForDedup: List<Transaction> = booklet.transactions
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

        // Use all physical transactions in range (preview + confirmed) for deduplication.
        // This prevents generating virtual occurrences when the month already has a physical counterpart.
        val relevantPhysicalTransactionsForDedup = allPhysicalTransactionsForDedup.filter { transaction ->
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
        // while excluding all occurrences already materialized physically.
        val virtualTransactions = regularTransactionGeneratorService.calculateVirtualTransactions(
            booklet.id!!,
            regularTransactions,
            currentMonth,
            currentYear,
            targetMonth,
            targetYear,
            existingPhysicalTransactions = relevantPhysicalTransactionsForDedup
        )

        // Combine preview physical transactions and non-duplicate virtual transactions for calculation
        // Real transactions are already in booklet.amount
        val allRelevantTransactions = relevantPreviewTransactions + virtualTransactions

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