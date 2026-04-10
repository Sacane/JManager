package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.BookletBalances
import fr.sacane.jmanager.domain.models.UserId
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
 * High-level API for managing booklets exposed to the application layer.
 * Implementations are responsible for authentication and returning domain Result<T>
 * signaling success or failure states.
 */
sealed interface BookletFeature {
    /**
     * Find a booklet by its unique identifier.
     *
     * @param bookletId The UUID of the booklet to find.
     * @param token Authentication token identifying the requester.
     * @return Result containing the found Booklet on success, or a failure state (e.g. BOOKLET_NOT_FOUND).
     */
    fun findBookletById(bookletId: UUID, token: String): Result<Booklet>

    /**
     * Edit an existing booklet.
     *
     * @param booklet The Booklet object containing updated values (must include an id).
     * @param token Authentication token identifying the requester.
     * @return Result containing the updated Booklet on success, or failure states when validation or persistence fails.
     */
    fun editBooklet(booklet: Booklet, token: String): Result<Booklet>

    /**
     * Delete a booklet by its identifier.
     *
     * @param bookletId The UUID of the booklet to delete.
     * @param token Authentication token identifying the requester.
     * @return Result with no value on success, or an error state if the booklet does not exist.
     */
    fun deleteBookletById(bookletId: UUID, token: String): Result<Nothing>

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
    fun findAllRegisteredBooklets(token: String): Result<List<Booklet>>

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
        startingYear: Int? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
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
        startingYear: Int? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
    ): Result<BookletBalances>

    /**
     * Re-generates previsional transactions for a given month that were previously deleted by the user.
     * Un-marks the month as excluded in the tracker so the generator can recreate them.
     * No duplicate is created if a confirmed or preview transaction already exists for that period.
     *
     * @param token Authentication token identifying the requester.
     * @param bookletId UUID of the booklet to target.
     * @param month Target month to regenerate.
     * @param year Target year to regenerate.
     * @return Result containing the list of newly created previsional transactions, or a failure state.
     */
    fun regenerateDeletedPrevisionalTransactions(
        token: String,
        bookletId: UUID,
        month: Month,
        year: Int
    ): Result<List<Transaction>>
}

@DomainService
class BookletFeatureImpl(
    private val userRepository: UserRepository,
    private val session: SessionManager,
    private val bookletRepository: BookletRepository,
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

    private fun userOwnsBooklet(userId: UserId, bookletId: UUID): Boolean {
        val userBooklets = bookletRepository.findBookletsForUser(userId)
        return userBooklets.any { it.id == bookletId }
    }

    override fun findBookletById(
        bookletId: UUID,
        token: String
    ): Result<Booklet> = session.authenticate(token) {
        bookletRepository.findBookletByIdWithTransactions(bookletId)?.run {
            success(this)
        } ?: domainFailure(
            ResultState.BOOKLET_NOT_FOUND,
            "Le compte est introuvable",
            "domain.booklet.find_by_id.not_found"
        )
    }

    override fun editBooklet(
        booklet: Booklet,
        token: String
    ): Result<Booklet> = session.authenticate(token) { userId ->
        val bookletID = booklet.id ?: return@authenticate domainFailure(
            ResultState.BOOKLET_NOT_FOUND,
            "Le livret ${booklet.label} est introuvable en base",
            "domain.booklet.edit.id_missing"
        )
        if (!userOwnsBooklet(userId, bookletID)) {
            return@authenticate domainFailure(
                ResultState.FORBIDDEN,
                "Vous n'avez pas accès à ce livret",
                "domain.booklet.edit.forbidden"
            )
        }
        val oldBooklet = bookletRepository.findBookletByIdWithTransactions(bookletID)
            ?: return@authenticate domainFailure(
                ResultState.BOOKLET_NOT_FOUND,
                "Le livret ${booklet.id} est introuvable",
                "domain.booklet.edit.not_found"
            )
        if(oldBooklet.id != booklet.id && oldBooklet.label == booklet.label){
            return@authenticate domainFailure(
                ResultState.BOOKLET_LABEL_EXIST,
                "Le libellé du livret existe déjà",
                "domain.booklet.edit.label_already_exists"
            )
        }
        oldBooklet.updateFrom(booklet)
        val registered = bookletRepository.upsert(oldBooklet)
        success(registered)
    }

    override fun deleteBookletById(
        bookletId: UUID,
        token: String
    ): Result<Nothing> = session.authenticate(token) { userId ->
        return@authenticate unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {
            if(bookletRepository.findBookletByIdWithTransactions(bookletId) == null){
                return@executeInTransaction domainFailure(
                    ResultState.NOT_FOUND,
                    "Le livret $bookletId n'existe pas",
                    "domain.booklet.delete.not_found"
                )
            }
            if (!userOwnsBooklet(userId, bookletId)) {
                return@executeInTransaction domainFailure(
                    ResultState.FORBIDDEN,
                    "Vous n'avez pas accès à ce livret",
                    "domain.booklet.delete.forbidden"
                )
            }
            bookletRepository.deleteBookletById(bookletId)
            trackerRepository.deleteTrackerByBookletId(bookletId)
            return@executeInTransaction success()
        }
    }

    override fun findByLabelAndUserId(
        token: String,
        label: String
    ): Result<Booklet> = session.authenticate(token) {
        val user = userRepository.findUserByIdWithBooklets(it)
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

    override fun findAllRegisteredBooklets(
        token: String
    ): Result<List<Booklet>> = session.authenticate(token) {
        val user = userRepository.findUserByIdWithBooklets(it)
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
        val user = userRepository.findUserByIdWithBooklets(it)
            ?: return@authenticate domainFailure(
                ResultState.USER_NOT_FOUND,
                "L'utilisateur n'existe pas en base",
                "domain.booklet.save.user_not_found"
            )
        if(user.hasBooklet(booklet.label)) {
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
        val bookletSaved = bookletRepository.save(it, booklet)
            ?: return@authenticate domainFailure(
                ResultState.INFRASTRUCTURE_ERROR,
                "Erreur lors de la sauvegarde du compte",
                "domain.booklet.save.infrastructure_error"
            )
        success(bookletSaved)
    }

    override fun loadTransactionsForBookletForAMonth(
        token: String,
        bookletId: UUID,
        month: Month,
        year: Int,
        startingMonth: Month?,
        startingYear: Int?,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): Result<BookletLoadingResult> = session.authenticate(token) { userId ->
        return@authenticate unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {
            val totalStartNs = System.nanoTime()
            LOGGER.info("Loading transactions for booklet $bookletId for month $month and year $year")

            val fetchBookletStartNs = System.nanoTime()
            val booklet: Booklet = bookletRepository.findBookletByIdWithTransactions(bookletId)
                ?: return@executeInTransaction domainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Requested booklet is not registered",
                    "domain.booklet.load_transactions.not_found"
                )
            LOGGER.info { "Fetched booklet: ${booklet.label} (${booklet.id})" }
            val fetchBookletMs = Duration.ofNanos(System.nanoTime() - fetchBookletStartNs).toMillis()

            val fetchRegularStartNs = System.nanoTime()
            val regularTransactions = regularTransactionRepository.getAllRegularUsedByBooklet(userId, bookletId)
                ?: emptyList()
            val fetchRegularMs = Duration.ofNanos(System.nanoTime() - fetchRegularStartNs).toMillis()

            val currentDate = LocalDate.now()
            val currentMonth = startingMonth ?: currentDate.month
            val currentYear = startingYear ?: currentDate.year
            val hasExplicitDateRange = startDate != null || endDate != null

            if ((startDate == null) != (endDate == null)) {
                return@executeInTransaction domainFailure(
                    ResultState.BAD_REQUEST,
                    "startDate and endDate must be both defined or both omitted",
                    "domain.booklet.load_transactions.invalid_date_range"
                )
            }

            if (startDate != null && startDate.isAfter(endDate)) {
                return@executeInTransaction domainFailure(
                    ResultState.BAD_REQUEST,
                    "startDate cannot be after endDate",
                    "domain.booklet.load_transactions.invalid_date_range"
                )
            }

            val resolvedRangeStart = startDate ?: LocalDate.of(year, month, 1)
            val resolvedRangeEnd = endDate ?: resolvedRangeStart.withDayOfMonth(resolvedRangeStart.lengthOfMonth())

            val targetYearMonth = YearMonth.of(year, month)
            val currentYearMonth = YearMonth.of(currentYear, currentMonth)
            val today = LocalDate.of(currentYear, currentMonth, currentDate.dayOfMonth)

            val generationStartNs = System.nanoTime()
            val generatedCount: Int = if (!hasExplicitDateRange && targetYearMonth.equals(currentYearMonth)) {
                val transactions = regularTransactionGeneratorService.generateMissingPrevisionalTransactions(
                    bookletId,
                    regularTransactions,
                    month,
                    year
                )
                LOGGER.info("Generated ${transactions.size} physical transactions for current month $month/$year")
                transactions.size
            } else if (hasExplicitDateRange) {
                // When a custom date range is provided, generate physical previsional transactions for
                // every calendar month covered by the range that qualifies as the "current" period
                // (i.e. today falls inside [resolvedRangeStart, resolvedRangeEnd]).
                if (!today.isBefore(resolvedRangeStart) && !today.isAfter(resolvedRangeEnd)) {
                    val startYM = YearMonth.from(resolvedRangeStart)
                    val endYM = YearMonth.from(resolvedRangeEnd)
                    var generated = 0
                    var ym = startYM
                    while (!ym.isAfter(endYM)) {
                        val boundsStart = if (ym == startYM) resolvedRangeStart else ym.atDay(1)
                        val boundsEnd = if (ym == endYM) resolvedRangeEnd else ym.atEndOfMonth()
                        val transactions = regularTransactionGeneratorService.generateMissingPrevisionalTransactions(
                            bookletId,
                            regularTransactions,
                            ym.month,
                            ym.year,
                            startDateBound = boundsStart,
                            endDateBound = boundsEnd
                        )
                        generated += transactions.size
                        ym = ym.plusMonths(1)
                    }
                    LOGGER.info("Generated $generated physical transactions for custom-range current period $resolvedRangeStart..$resolvedRangeEnd")
                    generated
                } else {
                    LOGGER.info("Skipping physical generation for custom-range non-current period $resolvedRangeStart..$resolvedRangeEnd")
                    0
                }
            } else {
                LOGGER.info("Skipping physical transaction generation for non-current month $month/$year")
                0
            }
            val generationMs = Duration.ofNanos(System.nanoTime() - generationStartNs).toMillis()

            val updateBookletMs = 0L

            val monthSheetStartNs = System.nanoTime()
            val allTransactionsForPeriod = transactionQueryRepository.findByBookletIdAndDateBetween(bookletId, resolvedRangeStart, resolvedRangeEnd)
            val monthSheetMs = Duration.ofNanos(System.nanoTime() - monthSheetStartNs).toMillis()

            val dedupRangeStart = if (hasExplicitDateRange) {
                resolvedRangeStart
            } else {
                LocalDate.of(currentYear, currentMonth, 1)
            }
            val allPhysicalTransactionsForDedup = if (dedupRangeStart < resolvedRangeStart) {
                transactionQueryRepository.findByBookletIdAndDateBetween(bookletId, dedupRangeStart, resolvedRangeEnd)
            } else {
                allTransactionsForPeriod
            }

            val preloadTrackersStartNs = System.nanoTime()
            val trackersByRegularId = trackerRepository.findAllTrackersForBooklet(bookletId)
                .associateBy { it.regularTransactionId }
            val preloadTrackersMs = Duration.ofNanos(System.nanoTime() - preloadTrackersStartNs).toMillis()

            val filterExcludedStartNs = System.nanoTime()
            val filteredTransactions = allTransactionsForPeriod.filter { transaction ->
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

            val virtualTransactionsForTargetPeriod = if (hasExplicitDateRange) {
                if (resolvedRangeEnd.isBefore(today)) {
                    emptyList()
                } else {
                    regularTransactionGeneratorService.calculateVirtualTransactions(
                        bookletId = bookletId,
                        regularTransactions = regularTransactions,
                        startMonth = resolvedRangeStart.month,
                        startYear = resolvedRangeStart.year,
                        endMonth = resolvedRangeEnd.month,
                        endYear = resolvedRangeEnd.year,
                        existingPhysicalTransactions = allTransactionsForPeriod
                    )
                        .filter { tx -> !tx.date.isBefore(resolvedRangeStart) && !tx.date.isAfter(resolvedRangeEnd) }
                }
            } else if (targetYearMonth.month == currentYearMonth.month) {
                emptyList()
            } else if (targetYearMonth.isBefore(currentYearMonth)) {
                emptyList()
            } else {
                regularTransactionGeneratorService.calculateVirtualTransactions(
                    bookletId = bookletId,
                    regularTransactions = regularTransactions,
                    startMonth = month,
                    startYear = year,
                    endMonth = month,
                    endYear = year,
                    existingPhysicalTransactions = allTransactionsForPeriod
                )
            }

            val combinedPrevisionalTransactions = (transactions.first + virtualTransactionsForTargetPeriod)
                .sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })

            val previsionalRangeStart = if (hasExplicitDateRange) {
                resolvedRangeStart
            } else {
                LocalDate.of(currentYear, currentMonth, 1)
            }

            val previsionalStartNs = System.nanoTime()
            val previsionalSold = calculatePrevisionalSold(
                booklet,
                regularTransactions,
                previsionalRangeStart,
                resolvedRangeEnd,
                allPhysicalTransactionsForDedup = allPhysicalTransactionsForDedup
            )
            val previsionalMs = Duration.ofNanos(System.nanoTime() - previsionalStartNs).toMillis()

            val filteredRegularTransactions = regularTransactions.filter { rt ->
                !rt.startDate.isAfter(resolvedRangeEnd)
            }

            val hasRegenerableTransactions = trackersByRegularId.values.any { tracker ->
                tracker.excludedMonths.contains(targetYearMonth)
            }

            val bookletLoadingResult = BookletLoadingResult(
                label = booklet.label,
                currentTransactions = transactions.second,
                previsionalTransactions = combinedPrevisionalTransactions,
                regularTransactions = filteredRegularTransactions,
                realSold = booklet.amount,
                previsionalSold = previsionalSold,
                hasRegenerableTransactions = hasRegenerableTransactions
            )

            val totalMs = Duration.ofNanos(System.nanoTime() - totalStartNs).toMillis()
            LOGGER.info(
                """
                Booklet loaded successfully:
                - bookletId: $bookletId
                - period: $month/$year (range=$resolvedRangeStart..$resolvedRangeEnd)
                - sizes: monthTransactions=${allTransactionsForPeriod.size}, current=${transactions.second.size}, preview=${transactions.first.size}, virtualPreview=${virtualTransactionsForTargetPeriod.size}, regular=${regularTransactions.size}, trackers=${trackersByRegularId.size}
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
        startingYear: Int?,
        startDate: LocalDate?,
        endDate: LocalDate?,
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

            if ((startDate == null) != (endDate == null)) {
                return@executeInTransaction domainFailure(
                    ResultState.BAD_REQUEST,
                    "startDate and endDate must be both defined or both omitted",
                    "domain.booklet.load_balances.invalid_date_range"
                )
            }

            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                return@executeInTransaction domainFailure(
                    ResultState.BAD_REQUEST,
                    "startDate cannot be after endDate",
                    "domain.booklet.load_balances.invalid_date_range"
                )
            }

            val regularTransactions = regularTransactionRepository.getAllRegularUsedByBooklet(userId, bookletId)
                ?: emptyList()

            val baseBooklet = Booklet(
                amount = Amount(persisted.amount),
                label = persisted.label,
                id = bookletId
            )

            val (from, to) = if (startDate != null && endDate != null) {
                startDate to endDate
            } else {
                val previewStart = YearMonth.of(currentYear, currentMonth)
                val previewEnd = YearMonth.of(year, month)
                monthDateBounds(previewStart, previewEnd)
            }
            val allPhysicalTransactionsInRange = transactionQueryRepository
                .findByBookletIdAndDateBetween(bookletId, from, to)

            val physicalPreviewTransactions = allPhysicalTransactionsInRange.filter { it.isPreview }
            physicalPreviewTransactions.forEach { baseBooklet.addTransaction(it) }

            val previsionalSold = calculatePrevisionalSold(
                baseBooklet,
                regularTransactions,
                from,
                to,
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

    override fun regenerateDeletedPrevisionalTransactions(
        token: String,
        bookletId: UUID,
        month: Month,
        year: Int
    ): Result<List<Transaction>> = session.authenticate(token) { userId ->
        return@authenticate unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {
            bookletRepository.findBookletByIdWithTransactions(bookletId)
                ?: return@executeInTransaction domainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Requested booklet is not registered",
                    "domain.booklet.regenerate.not_found"
                )
            val regularTransactions = regularTransactionRepository.getAllRegularUsedByBooklet(userId, bookletId)
                ?: emptyList()
            val targetYearMonth = YearMonth.of(year, month)
            regularTransactions.forEach { rt ->
                val tracker = trackerRepository.findTracker(rt.id, bookletId)
                if (tracker?.excludedMonths?.contains(targetYearMonth) == true) {
                    trackerRepository.unmarkMonthAsExcluded(rt.id, bookletId, year, month)
                }
            }
            val regenerated = regularTransactionGeneratorService.generateMissingPrevisionalTransactions(
                bookletId,
                regularTransactions,
                month,
                year
            )
            return@executeInTransaction success(regenerated)
        }
    }

    private fun monthDateBounds(start: YearMonth, end: YearMonth): Pair<LocalDate, LocalDate> {
        val from = minOf(start, end).atDay(1)
        val to = maxOf(start, end).atEndOfMonth()
        return from to to
    }

    private fun calculatePrevisionalSold(
        booklet: Booklet,
        regularTransactions: List<RegularTransaction>,
        rangeStartDate: LocalDate,
        rangeEndDate: LocalDate,
        allPhysicalTransactionsForDedup: List<Transaction> = booklet.transactions
    ): Amount {
        val allTransactions = booklet.transactions

        val relevantPreviewTransactions = allTransactions
            .filter { tx -> !tx.date.isBefore(rangeStartDate) && !tx.date.isAfter(rangeEndDate) }
            .filter { it.isPreview }

        val relevantPhysicalTransactionsForDedup = allPhysicalTransactionsForDedup
            .filter { tx -> !tx.date.isBefore(rangeStartDate) && !tx.date.isAfter(rangeEndDate) }

        val virtualTransactions = regularTransactionGeneratorService.calculateVirtualTransactions(
            booklet.id!!,
            regularTransactions,
            rangeStartDate.month,
            rangeStartDate.year,
            rangeEndDate.month,
            rangeEndDate.year,
            existingPhysicalTransactions = relevantPhysicalTransactionsForDedup
        ).filter { tx -> !tx.date.isBefore(rangeStartDate) && !tx.date.isAfter(rangeEndDate) }

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
    val previsionalSold: Amount,
    val hasRegenerableTransactions: Boolean = false
)