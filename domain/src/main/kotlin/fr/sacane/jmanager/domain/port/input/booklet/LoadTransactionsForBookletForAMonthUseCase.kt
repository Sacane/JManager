package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.Paginator
import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.TransactionSortDirection
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionQueryRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.port.input.MdcKeys
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import org.slf4j.LoggerFactory

data class LoadTransactionsForBookletForAMonthQuery(
    val userId: UserId,
    val bookletId: UUID,
    val month: Month,
    val year: Int,
    val startingMonth: Month? = null,
    val startingYear: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val pageNumber: Int = 0,
    val pageSize: Int = 10,
    val sortDirection: TransactionSortDirection? = null,
) : Query<BookletLoadingResult>, MdcContextProvider {
    override fun mdcContext() = mapOf(MdcKeys.BOOKLET_ID to bookletId.toString())
}

@Port(Side.APPLICATION)
interface LoadTransactionsForBookletForAMonthUseCase : QueryHandler<LoadTransactionsForBookletForAMonthQuery, BookletLoadingResult> {
    override val queryClass get() = LoadTransactionsForBookletForAMonthQuery::class
}

@DomainService
class LoadTransactionsForBookletForAMonthService(
    private val bookletRepository: BookletRepository,
    private val regularTransactionRepository: RegularTransactionRepository,
    private val regularTransactionGeneratorService: RegularTransactionGenerator,
    private val unitOfWorkTransactionProviderPort: UnitOfWorkTransactionProvider,
    private val trackerRepository: RegularTransactionTrackerRepository,
    private val transactionQueryRepository: TransactionQueryRepository,
    private val paginator: Paginator
) : LoadTransactionsForBookletForAMonthUseCase {

    companion object {
        private val log = LoggerFactory.getLogger(LoadTransactionsForBookletForAMonthService::class.java)
        private val BY_DATE_THEN_LAST_MODIFIED = compareBy<Transaction> { it.date }.thenBy { it.lastModified }
    }

    override fun handle(query: LoadTransactionsForBookletForAMonthQuery): Result<BookletLoadingResult> {
        val (userId, bookletId, month, year, startingMonth, startingYear, startDate, endDate, pageNumber, pageSize) = query
        return unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {
            val totalStartNs = System.nanoTime()
            log.info("Loading transactions for {}/{}", month, year)

            val fetchBookletStartNs = System.nanoTime()
            val booklet: Booklet = bookletRepository.findBookletByIdWithTransactions(bookletId)
                ?: return@executeInTransaction bookletDomainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Requested booklet is not registered",
                    "domain.booklet.load_transactions.not_found"
                )
            log.info("Fetched booklet: label={}", booklet.label)
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
                return@executeInTransaction bookletDomainFailure(
                    ResultState.BAD_REQUEST,
                    "startDate and endDate must be both defined or both omitted",
                    "domain.booklet.load_transactions.invalid_date_range"
                )
            }

            if (startDate != null && startDate.isAfter(endDate)) {
                return@executeInTransaction bookletDomainFailure(
                    ResultState.BAD_REQUEST,
                    "startDate cannot be after endDate",
                    "domain.booklet.load_transactions.invalid_date_range"
                )
            }

            val resolvedRangeStart = startDate ?: LocalDate.of(year, month, 1)
            val resolvedRangeEnd = endDate ?: resolvedRangeStart.withDayOfMonth(resolvedRangeStart.lengthOfMonth())

            val targetYearMonth = YearMonth.of(year, month)
            val currentYearMonth = YearMonth.of(currentYear, currentMonth)
            val currentYM = YearMonth.of(currentYear, currentMonth)
            val today = currentYM.atDay(minOf(currentDate.dayOfMonth, currentYM.lengthOfMonth()))

            val generationStartNs = System.nanoTime()
            val generatedCount: Int = if (!hasExplicitDateRange && targetYearMonth.equals(currentYearMonth)) {
                val transactions = regularTransactionGeneratorService.generateMissingPrevisionalTransactions(
                    bookletId,
                    regularTransactions,
                    month,
                    year
                )
                log.info("Generated {} physical transactions for current month {}/{}", transactions.size, month, year)
                transactions.size
            } else if (hasExplicitDateRange) {
                if (!today.isBefore(resolvedRangeStart) && !YearMonth.from(today).isAfter(YearMonth.from(resolvedRangeEnd))) {
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
                    log.info("Generated {} physical transactions for custom range {}..{}", generated, resolvedRangeStart, resolvedRangeEnd)
                    generated
                } else {
                    log.info("Skipping generation for non-current custom range {}..{}", resolvedRangeStart, resolvedRangeEnd)
                    0
                }
            } else {
                log.info("Skipping generation for non-current month {}/{}", month, year)
                0
            }
            val generationMs = Duration.ofNanos(System.nanoTime() - generationStartNs).toMillis()

            val updateBookletMs = 0L

            val monthTransactionStartNs = System.nanoTime()
            val allTransactionsForPeriod = transactionQueryRepository.findByBookletIdAndDateBetween(bookletId, resolvedRangeStart, resolvedRangeEnd)
            val monthTransactionMs = Duration.ofNanos(System.nanoTime() - monthTransactionStartNs).toMillis()

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
                .sortedWith(BY_DATE_THEN_LAST_MODIFIED)

            val previsionalRangeStart = if (hasExplicitDateRange) {
                resolvedRangeStart
            } else {
                LocalDate.of(currentYear, currentMonth, 1)
            }

            val previsionalStartNs = System.nanoTime()
            val previsionalSold = calculatePrevisionalSold(
                regularTransactionGeneratorService,
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

            val hasRegenerableTransactions = !targetYearMonth.isBefore(YearMonth.now()) &&
                trackersByRegularId.values.any { tracker ->
                    tracker.excludedMonths.contains(targetYearMonth)
                }

            val allDisplayTransactions = sortForDisplay(
                transactions.second + combinedPrevisionalTransactions,
                query.sortDirection
            )
            val page = paginator.paginate(pageNumber, pageSize) { allDisplayTransactions }
            val pagedCurrentTransactions = page.content.filter { !it.isPreview }
            val pagedPrevisionalTransactions = page.content.filter { it.isPreview }

            val bookletLoadingResult = BookletLoadingResult(
                label = booklet.label,
                currentTransactions = pagedCurrentTransactions,
                previsionalTransactions = pagedPrevisionalTransactions,
                regularTransactions = filteredRegularTransactions,
                realSold = booklet.amount,
                previsionalSold = previsionalSold,
                hasRegenerableTransactions = hasRegenerableTransactions,
                pageNumber = page.pageNumber,
                pageSize = page.pageSize,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                orderedTransactions = page.content,
            )

            val totalMs = Duration.ofNanos(System.nanoTime() - totalStartNs).toMillis()
            log.info(
                "Booklet loaded: period={}/{} range={}..{} | sizes: month={} current={} preview={} virtual={} regular={} trackers={} | timings(ms): fetchBooklet={} fetchRegular={} generate={}(n={}) monthQuery={} trackers={} filterExcluded={} previsional={} total={}",
                month, year, resolvedRangeStart, resolvedRangeEnd,
                allTransactionsForPeriod.size, transactions.second.size, transactions.first.size,
                virtualTransactionsForTargetPeriod.size, regularTransactions.size, trackersByRegularId.size,
                fetchBookletMs, fetchRegularMs, generationMs, generatedCount,
                monthTransactionMs, preloadTrackersMs, filterExcludedMs, previsionalMs, totalMs
            )

            return@executeInTransaction success(bookletLoadingResult)
        }
    }

    private fun sortForDisplay(
        transactions: List<Transaction>,
        sortDirection: TransactionSortDirection?
    ): List<Transaction> {
        return when (sortDirection) {
            null -> transactions
            TransactionSortDirection.ASCENDING -> transactions.sortedWith(BY_DATE_THEN_LAST_MODIFIED)
            TransactionSortDirection.DESCENDING -> transactions.sortedWith(BY_DATE_THEN_LAST_MODIFIED.reversed())
        }
    }
}
