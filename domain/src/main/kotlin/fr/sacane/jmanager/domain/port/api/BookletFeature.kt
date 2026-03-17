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
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
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
     * @param bookletId UUID of the booklet to load transactions for.
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

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return fr.sacane.jmanager.domain.utils.failure(state, DomainError(state.code, key, detail))
    }

    override fun findAccountById(
        accountID: UUID,
        token: String
    ): Result<Booklet> = session.authenticate(token) {
        accountRepository.findAccountByIdWithTransactions(accountID)?.run {
            success(this)
        } ?: domainFailure(
            ResultState.BOOKLET_NOT_FOUND,
            "Le compte est introuvable",
            "domain.booklet.find_by_id.not_found"
        )
    }

    override fun editAccount(
        booklet: Booklet,
        token: String
    ): Result<Booklet> = session.authenticate(token) {
        val accountID = booklet.id ?: return@authenticate domainFailure(
            ResultState.BOOKLET_NOT_FOUND,
            "Le livret ${booklet.label} est introuvable en base",
            "domain.booklet.edit.id_missing"
        )
        val oldAccount = accountRepository.findAccountByIdWithTransactions(accountID)
            ?: return@authenticate domainFailure(
                ResultState.BOOKLET_NOT_FOUND,
                "Le livret ${booklet.id} est introuvable",
                "domain.booklet.edit.not_found"
            )
        if(oldAccount.id != booklet.id && oldAccount.label == booklet.label){
            return@authenticate domainFailure(
                ResultState.BOOKLET_LABEL_EXIST,
                "Le libellé du livret existe déjà",
                "domain.booklet.edit.label_already_exists"
            )
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
                return@executeInTransaction domainFailure(
                    ResultState.NOT_FOUND,
                    "Le livret $accountID n'existe pas",
                    "domain.booklet.delete.not_found"
                )
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
            ?: return@authenticate domainFailure(
                ResultState.USER_NOT_FOUND,
                "L'utilisateur recherché n'existe pas",
                "domain.booklet.find_by_label.user_not_found"
            )
        success(
            user.booklets
            .find { acc -> acc.label == label }
            ?: return@authenticate domainFailure(
                ResultState.BOOKLET_LABEL_NOT_EXIST,
                "Le compte $label n'est pas enregistré en base",
                "domain.booklet.find_by_label.label_not_found"
            )
        )
    }

    override fun findAllRegisteredAccounts(
        token: String
    ): Result<List<Booklet>> = session.authenticate(token) {
        val user = userRepository.findUserByIdWithAccounts(it)
            ?: return@authenticate domainFailure(
                ResultState.BOOKLET_NOT_FOUND,
                "L'utilisateur n'existe pas en base",
                "domain.booklet.find_all.user_not_found"
            )
        return@authenticate success(user.booklets)
    }

    override fun save(
        token: String,
        booklet: Booklet
    ): Result<Booklet> = session.authenticate(token) {
        val user = userRepository.findUserByIdWithAccounts(it)
            ?: return@authenticate domainFailure(
                ResultState.USER_NOT_FOUND,
                "L'utilisateur n'existe pas en base",
                "domain.booklet.save.user_not_found"
            )
        if(user.hasAccount(booklet.label)) {
            return@authenticate domainFailure(
                ResultState.BOOKLET_LABEL_EXIST,
                "Le profil contient déjà un compte avec le label ${booklet.label}",
                "domain.booklet.save.label_already_exists"
            )
        }
        if (user.booklets.size >= 6) {
            return@authenticate domainFailure(
                ResultState.BOOKLET_MAXIMUM_SIZE_REACHED,
                "Le profil ne peut pas contenir plus de 6 comptes",
                "domain.booklet.save.maximum_size_reached"
            )
        }
        val accountSaved = accountRepository.save(it, booklet)
            ?: return@authenticate domainFailure(
                ResultState.INFRASTRUCTURE_ERROR,
                "Erreur lors de la sauvegarde du compte",
                "domain.booklet.save.infrastructure_error"
            )
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
                ?: return@executeInTransaction domainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Requested booklet is not registered",
                    "domain.booklet.load_transactions.not_found"
                )
            LOGGER.info { "Fetched booklet: ${booklet.label} (${booklet.id})" }
            val fetchBookletMs = Duration.ofNanos(System.nanoTime() - fetchBookletStartNs).toMillis()

            val fetchRegularStartNs = System.nanoTime()
            val regularTransactions = regularTransactionRepository.getAllRegularUsedByAccount(userId, bookletId)
                ?: emptyList()
            val fetchRegularMs = Duration.ofNanos(System.nanoTime() - fetchRegularStartNs).toMillis()

            val currentDate = LocalDate.now()
            val currentMonth = startingMonth ?: currentDate.month
            val currentYear = startingYear ?: currentDate.year

            val targetYearMonth = YearMonth.of(year, month)
            val currentYearMonth = YearMonth.of(currentYear, currentMonth)

            val generationStartNs = System.nanoTime()
            val generatedCount: Int = if (targetYearMonth.equals(currentYearMonth)) {
                val transactions = regularTransactionGeneratorService.generateMissingPrevisionalTransactions(
                    bookletId,
                    regularTransactions,
                    month,
                    year
                )
                LOGGER.info("Generated ${transactions.size} physical transactions for current month $month/$year")
                transactions.size
            } else {
                LOGGER.info("Skipping physical transaction generation for non-current month $month/$year")
                0
            }
            val generationMs = Duration.ofNanos(System.nanoTime() - generationStartNs).toMillis()

            val updateBookletMs = 0L

            val monthSheetStartNs = System.nanoTime()
            val rangeStart = LocalDate.of(year, month, 1)
            val rangeEnd = rangeStart.withDayOfMonth(rangeStart.lengthOfMonth())
            val allTransactionsForMonth = transactionQueryRepository.findByBookletIdAndDateBetween(bookletId, rangeStart, rangeEnd)
            val monthSheetMs = Duration.ofNanos(System.nanoTime() - monthSheetStartNs).toMillis()

            val dedupRangeStart = LocalDate.of(currentYear, currentMonth, 1)
            val allPhysicalTransactionsForDedup = if (dedupRangeStart < rangeStart) {
                transactionQueryRepository.findByBookletIdAndDateBetween(bookletId, dedupRangeStart, rangeEnd)
            } else {
                allTransactionsForMonth
            }

            val preloadTrackersStartNs = System.nanoTime()
            val trackersByRegularId = trackerRepository.findAllTrackersForBooklet(bookletId)
                .associateBy { it.regularTransactionId }
            val preloadTrackersMs = Duration.ofNanos(System.nanoTime() - preloadTrackersStartNs).toMillis()

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

            val virtualTransactionsForTargetMonth = if (targetYearMonth == currentYearMonth) {
                emptyList()
            } else {
                regularTransactionGeneratorService.calculateVirtualTransactions(
                    bookletId = bookletId,
                    regularTransactions = regularTransactions,
                    startMonth = month,
                    startYear = year,
                    endMonth = month,
                    endYear = year,
                    existingPhysicalTransactions = allTransactionsForMonth
                )
            }

            val combinedPrevisionalTransactions = (transactions.first + virtualTransactionsForTargetMonth)
                .sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })

            val previsionalStartNs = System.nanoTime()
            val previsionalSold = calculatePrevisionalSold(
                booklet,
                regularTransactions,
                currentMonth,
                currentYear,
                month,
                year,
                allPhysicalTransactionsForDedup = allPhysicalTransactionsForDedup
            )
            val previsionalMs = Duration.ofNanos(System.nanoTime() - previsionalStartNs).toMillis()

            val requestedDate = LocalDate.of(year, month, 1)
            val filteredRegularTransactions = regularTransactions.filter { rt ->
                !rt.startDate.isAfter(requestedDate.withDayOfMonth(requestedDate.lengthOfMonth()))
            }

            val bookletLoadingResult = BookletLoadingResult(
                label = booklet.label,
                currentTransactions = transactions.second,
                previsionalTransactions = combinedPrevisionalTransactions,
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
                - sizes: monthTransactions=${allTransactionsForMonth.size}, current=${transactions.second.size}, preview=${transactions.first.size}, virtualPreview=${virtualTransactionsForTargetMonth.size}, regular=${regularTransactions.size}, trackers=${trackersByRegularId.size}
                - timings(ms): fetchBooklet=$fetchBookletMs, fetchRegular=$fetchRegularMs, generate=$generationMs (generated=$generatedCount), updateBooklet=$updateBookletMs, monthQuery=$monthSheetMs, preloadTrackers=$preloadTrackersMs, filterExcluded=$filterExcludedMs, previsionalSold=$previsionalMs, total=$totalMs
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
                ?: return@executeInTransaction domainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Requested booklet is not registered",
                    "domain.booklet.load_balances.not_found"
                )

            val currentDate = LocalDate.now()
            val currentMonth = startingMonth ?: currentDate.month
            val currentYear = startingYear ?: currentDate.year

            val regularTransactions = regularTransactionRepository.getAllRegularUsedByAccount(userId, bookletId)
                ?: emptyList()

            val baseBooklet = Booklet(
                amount = Amount(persisted.amount),
                labelAccount = persisted.label,
                id = bookletId
            )

            val previewStart = YearMonth.of(currentYear, currentMonth)
            val previewEnd = YearMonth.of(year, month)
            val (from, to) = monthDateBounds(previewStart, previewEnd)
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

    private fun monthDateBounds(start: YearMonth, end: YearMonth): Pair<LocalDate, LocalDate> {
        val from = minOf(start, end).atDay(1)
        val to = maxOf(start, end).atEndOfMonth()
        return from to to
    }

    private fun isInRange(date: LocalDate, start: YearMonth, end: YearMonth): Boolean {
        val dateMonth = YearMonth.from(date)
        return dateMonth in start..end
    }

    private fun transactionsInRange(transactions: List<Transaction>, start: YearMonth, end: YearMonth): List<Transaction> {
        return transactions.filter { isInRange(it.date, start, end) }
    }

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
        val rangeStart = YearMonth.of(currentYear, currentMonth)
        val rangeEnd = YearMonth.of(targetYear, targetMonth)

        val relevantPreviewTransactions = transactionsInRange(allTransactions, rangeStart, rangeEnd)
            .filter { it.isPreview }

        val relevantPhysicalTransactionsForDedup = transactionsInRange(allPhysicalTransactionsForDedup, rangeStart, rangeEnd)

        val virtualTransactions = regularTransactionGeneratorService.calculateVirtualTransactions(
            booklet.id!!,
            regularTransactions,
            currentMonth,
            currentYear,
            targetMonth,
            targetYear,
            existingPhysicalTransactions = relevantPhysicalTransactionsForDedup
        )

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