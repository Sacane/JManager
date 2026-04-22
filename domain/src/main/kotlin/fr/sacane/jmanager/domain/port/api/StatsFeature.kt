package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.CategoryDistributionOutput
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.DailyTrendStatsOutput
import fr.sacane.jmanager.domain.models.MonthlyBookletStatsOutput
import fr.sacane.jmanager.domain.models.PrevisionalTransactionsOutput
import fr.sacane.jmanager.domain.models.TrendStatsOutput
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.usecase.CategoryDistributionCalculator
import fr.sacane.jmanager.domain.usecase.DailyTrendCalculator
import fr.sacane.jmanager.domain.usecase.MonthlyStatsCalculator
import fr.sacane.jmanager.domain.usecase.PrevisionalTransactionFilter
import fr.sacane.jmanager.domain.usecase.TrendCalculator
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.logging.Logger

@Port(Side.APPLICATION)
/**
 * Application port: StatsFeature
 *
 * High-level API exposing statistical and reporting use-cases for the authenticated user.
 * Implementations must authenticate requests and return domain Result<T> objects representing
 * success or domain-specific failure states.
 */
sealed interface StatsFeature {
    /**
     * Retrieves the monthly booklet statistics for a specific booklet and year.
     *
     * @param bookletId The unique identifier of the booklet.
     * @param year The year for which the statistics are to be retrieved.
     * @param token The authentication token for verifying requester access.
     * @return A Result object containing the monthly booklet statistics wrapped in
     *         MonthlyBookletStatsOutput if successful, or an appropriate error state otherwise.
     */
    fun getMonthlyBookletStats(bookletId: UUID, year: Int, token: SessionToken): Result<MonthlyBookletStatsOutput>

    /**
     * Retrieves the distribution of expenses across various categories for the authenticated user.
     *
     * @param token The authentication token used to verify the user's identity and access permissions.
     * @return A Result object containing the category distribution data wrapped in CategoryDistributionOutput
     *         if successful, or an appropriate error state otherwise.
     */
    fun getCategoryDistribution(
        token: SessionToken,
        bookletId: UUID? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<CategoryDistributionOutput>

    /**
     * Retrieves trend statistics for the authenticated user.
     * The trend statistics include aggregated data for trends over a period of time.
     *
     * @param token The authentication token used to verify the user's identity and access permissions.
     * @return A Result object containing the trend statistics wrapped in TrendStatsOutput if successful,
     *         or an appropriate error state otherwise.
     */
    fun getTrendStats(
        token: SessionToken,
        bookletId: UUID? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<TrendStatsOutput>

    /**
     * Retrieves a set of provisional transactions for the authenticated user within a specified date range.
     *
     * @param token The authentication token used to verify the user's identity and access permissions.
     * @param startDate The starting date of the range for which provisional transactions are requested.
     * @param endDate The ending date of the range for which provisional transactions are requested.
     * @return A Result object containing the provisional transactions data wrapped in PrevisionalTransactionsOutput
     *         if successful, or an appropriate error state otherwise.
     */
    fun getPrevisionalTransactions(
        token: SessionToken,
        startDate: LocalDate,
        endDate: LocalDate,
        bookletId: UUID? = null
    ): Result<PrevisionalTransactionsOutput>

    /**
     * Retrieves daily trend statistics for the authenticated user within a specified date range.
     *
     * @param token The authentication token used to verify the user's identity and access permissions.
     * @param startDate The starting date of the range (inclusive, required).
     * @param endDate The ending date of the range (inclusive, required).
     * @param bookletId Optional booklet identifier to scope the calculation.
     * @return A Result object containing daily trend data wrapped in DailyTrendStatsOutput.
     */
    fun getDailyTrendStats(
        token: SessionToken,
        startDate: LocalDate,
        endDate: LocalDate,
        bookletId: UUID? = null
    ): Result<DailyTrendStatsOutput>
}


@DomainService
class StatsFeatureImpl(
    private val session: SessionManager,
    private val userRepository: UserRepository,
    private val bookletRepository: BookletRepository,
    private val monthlyStatsCalculator: MonthlyStatsCalculator,
    private val categoryDistributionCalculator: CategoryDistributionCalculator,
    private val trendCalculator: TrendCalculator,
    private val dailyTrendCalculator: DailyTrendCalculator,
    private val previsionalTransactionFilter: PrevisionalTransactionFilter
) : StatsFeature {
    companion object {
        private val LOGGER = Logger.getLogger(StatsFeatureImpl::class.java.name)
    }

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return fr.sacane.jmanager.domain.utils.failure(state, DomainError(state.code, key, detail))
    }

    private fun validateDateRange(startDate: LocalDate?, endDate: LocalDate?, keyPrefix: String): Pair<String, String>? {
        if ((startDate == null) != (endDate == null)) {
            return Pair(
                "La date de début et la date de fin doivent être fournies ensemble",
                "$keyPrefix.invalid_partial_date_range"
            )
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return Pair(
                "La date de début doit être antérieure à la date de fin",
                "$keyPrefix.invalid_date_range"
            )
        }

        return null
    }

    private fun <S> withScopedBooklets(
        userId: UserId,
        bookletId: UUID?,
        onSuccess: (List<Booklet>) -> Result<S>
    ): Result<S> {
        if (bookletId == null) {
            return onSuccess(bookletRepository.findBookletsForUser(userId))
        }

        val booklet = bookletRepository.findBookletByIdWithTransactions(bookletId)
            ?: return domainFailure(
                ResultState.BOOKLET_NOT_FOUND,
                "Le livret $bookletId est introuvable",
                "domain.stats.booklet_not_found"
            )

        if (booklet.owner?.id != userId) {
            return domainFailure(
                ResultState.FORBIDDEN,
                "Vous n'avez pas accès à ce livret",
                "domain.stats.forbidden"
            )
        }

        return onSuccess(listOf(booklet))
    }

    override fun getMonthlyBookletStats(
        bookletId: UUID,
        year: Int,
        token: SessionToken
    ): Result<MonthlyBookletStatsOutput> = session.authenticate(token) { userId ->
        LOGGER.info("Fetching monthly stats for booklet $bookletId and year $year")

        val booklet = bookletRepository.findBookletByIdWithTransactions(bookletId)
            ?: return@authenticate domainFailure(
                ResultState.BOOKLET_NOT_FOUND,
                "Le livret $bookletId est introuvable",
                "domain.stats.monthly.booklet_not_found"
            )

        if (booklet.owner?.id != userId) {
            return@authenticate domainFailure(
                ResultState.FORBIDDEN,
                "Vous n'avez pas accès à ce livret",
                "domain.stats.monthly.forbidden"
            )
        }

        val monthlyData = monthlyStatsCalculator.calculateMonthlyStats(
            transactions = booklet.transactions,
            year = year
        )

        LOGGER.info("Monthly stats calculated: ${monthlyData.size} months processed")

        success(
            MonthlyBookletStatsOutput(
                bookletId = bookletId,
                bookletLabel = booklet.label,
                year = year,
                monthlyData = monthlyData
            )
        )
    }

    override fun getCategoryDistribution(
        token: SessionToken,
        bookletId: UUID?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Result<CategoryDistributionOutput> = session.authenticate(token) { userId ->
        LOGGER.info("Fetching category distribution for user $userId")

        validateDateRange(startDate, endDate, "domain.stats.category_distribution")?.let { (detail, key) ->
            return@authenticate domainFailure(ResultState.INVALID, detail, key)
        }

        withScopedBooklets(userId, bookletId) { scopedBooklets ->
            val allTransactions = scopedBooklets.flatMap { booklet ->
                booklet.transactions
            }

            LOGGER.info("All transactions fetched: ${allTransactions.size} transactions found")

            val (categories, totalExpenses) = categoryDistributionCalculator.calculateDistribution(
                transactions = allTransactions,
                startDate = startDate,
                endDate = endDate
            )

            LOGGER.info("Category distribution calculated: ${categories.size} categories found")

            success(
                CategoryDistributionOutput(
                    categories = categories,
                    totalExpenses = totalExpenses
                )
            )
        }
    }

    override fun getTrendStats(
        token: SessionToken,
        bookletId: UUID?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Result<TrendStatsOutput> = session.authenticate(token) { userId ->
        LOGGER.info("Fetching trend stats for user $userId")

        validateDateRange(startDate, endDate, "domain.stats.trend")?.let { (detail, key) ->
            return@authenticate domainFailure(ResultState.INVALID, detail, key)
        }

        withScopedBooklets(userId, bookletId) { scopedBooklets ->
            val monthlyTrends = trendCalculator.calculateTrend(
                booklets = scopedBooklets,
                startDate = startDate,
                endDate = endDate
            )

            LOGGER.info("Trend stats calculated: ${monthlyTrends.size} months processed")

            for (trend in monthlyTrends) {
                if (trend.income.value > BigDecimal.ZERO || trend.expenses.value > BigDecimal.ZERO) {
                    LOGGER.info(
                        "Month: ${trend.year}-${trend.month} | Income: ${trend.income} | Expenses: ${trend.expenses}"
                    )
                }
            }

            success(
                TrendStatsOutput(
                    monthlyTrends = monthlyTrends
                )
            )
        }
    }

    override fun getPrevisionalTransactions(
        token: SessionToken,
        startDate: LocalDate,
        endDate: LocalDate,
        bookletId: UUID?
    ): Result<PrevisionalTransactionsOutput> = session.authenticate(token) { userId ->
        LOGGER.info("Fetching previsional transactions from $startDate to $endDate for user $userId")

        if (startDate.isAfter(endDate)) {
            return@authenticate domainFailure(
                ResultState.INVALID,
                "La date de début doit être antérieure à la date de fin",
                "domain.stats.previsional.invalid_date_range"
            )
        }

        withScopedBooklets(userId, bookletId) { scopedBooklets ->
            val result = previsionalTransactionFilter.filterPrevisionalTransactions(
                booklets = scopedBooklets,
                startDate = startDate,
                endDate = endDate
            )

            LOGGER.info(
                """
                Previsional transactions fetched:
                - Total transactions: ${result.transactions.size}
                - Total amount: ${result.totalAmount}
                - Total income: ${result.totalIncome}
                - Total expenses: ${result.totalExpenses}
                """.trimIndent()
            )

            success(
                PrevisionalTransactionsOutput(
                    transactions = result.transactions,
                    groupedByBooklet = result.groupedByBooklet,
                    totalAmount = result.totalAmount,
                    totalIncome = result.totalIncome,
                    totalExpenses = result.totalExpenses,
                    regularTransactions = result.regularTransactions,
                    nonRegularTransactions = result.nonRegularTransactions,
                    totalRegularAmount = result.totalRegularAmount,
                    totalNonRegularAmount = result.totalNonRegularAmount,
                    startDate = startDate,
                    endDate = endDate
                )
            )
        }
    }

    override fun getDailyTrendStats(
        token: SessionToken,
        startDate: LocalDate,
        endDate: LocalDate,
        bookletId: UUID?
    ): Result<DailyTrendStatsOutput> = session.authenticate(token) { userId ->
        LOGGER.info("Fetching daily trend stats from $startDate to $endDate for user $userId")

        if (startDate.isAfter(endDate)) {
            return@authenticate domainFailure(
                ResultState.INVALID,
                "La date de début doit être antérieure à la date de fin",
                "domain.stats.daily_trend.invalid_date_range"
            )
        }

        withScopedBooklets(userId, bookletId) { scopedBooklets ->
            val dailyTrends = dailyTrendCalculator.calculateDailyTrend(
                booklets = scopedBooklets,
                startDate = startDate,
                endDate = endDate
            )

            LOGGER.info("Daily trend stats calculated: ${dailyTrends.size} days processed")

            success(
                DailyTrendStatsOutput(
                    dailyTrends = dailyTrends
                )
            )
        }
    }
}