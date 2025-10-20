package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.CategoryDistributionOutput
import fr.sacane.jmanager.domain.models.MonthlyAccountStatsOutput
import fr.sacane.jmanager.domain.models.PrevisionalTransactionsOutput
import fr.sacane.jmanager.domain.models.TrendStatsOutput
import fr.sacane.jmanager.domain.port.spi.BookletRepositoryPort
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.usecase.CategoryDistributionCalculator
import fr.sacane.jmanager.domain.usecase.MonthlyStatsCalculator
import fr.sacane.jmanager.domain.usecase.PrevisionalTransactionFilter
import fr.sacane.jmanager.domain.usecase.TrendCalculator
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.math.BigDecimal
import java.time.LocalDate
import java.util.logging.Logger

@Port(Side.APPLICATION)
sealed interface StatsFeature {
    /**
     * Retrieves the monthly account statistics for a specific account and year.
     *
     * @param accountId The unique identifier of the account.
     * @param year The year for which the statistics are to be retrieved.
     * @param token The authentication token for verifying requester access.
     * @return A Result object containing the monthly account statistics wrapped in
     *         MonthlyAccountStatsOutput if successful, or an appropriate error state otherwise.
     */
    fun getMonthlyAccountStats(accountId: Long, year: Int, token: String): Result<MonthlyAccountStatsOutput>

    /**
     * Retrieves the distribution of expenses across various categories for the authenticated user.
     *
     * @param token The authentication token used to verify the user's identity and access permissions.
     * @return A Result object containing the category distribution data wrapped in CategoryDistributionOutput
     *         if successful, or an appropriate error state otherwise.
     */
    fun getCategoryDistribution(token: String): Result<CategoryDistributionOutput>

    /**
     * Retrieves trend statistics for the authenticated user.
     * The trend statistics include aggregated data for trends over a period of time.
     *
     * @param token The authentication token used to verify the user's identity and access permissions.
     * @return A Result object containing the trend statistics wrapped in TrendStatsOutput if successful,
     *         or an appropriate error state otherwise.
     */
    fun getTrendStats(token: String): Result<TrendStatsOutput>

    /**
     * Retrieves a set of provisional transactions for the authenticated user within a specified date range.
     *
     * @param token The authentication token used to verify the user's identity and access permissions.
     * @param startDate The starting date of the range for which provisional transactions are requested.
     * @param endDate The ending date of the range for which provisional transactions are requested.
     * @return A Result object containing the provisional transactions data wrapped in PrevisionalTransactionsOutput
     *         if successful, or an appropriate error state otherwise.
     */
    fun getPrevisionalTransactions(token: String, startDate: LocalDate, endDate: LocalDate): Result<PrevisionalTransactionsOutput>
}


@DomainService
class StatsFeatureImpl(
    private val session: SessionManager,
    private val userRepository: UserRepository,
    private val bookletRepository: BookletRepositoryPort,
    private val monthlyStatsCalculator: MonthlyStatsCalculator,
    private val categoryDistributionCalculator: CategoryDistributionCalculator,
    private val trendCalculator: TrendCalculator,
    private val previsionalTransactionFilter: PrevisionalTransactionFilter
) : StatsFeature {

    companion object {
        private val LOGGER = Logger.getLogger(StatsFeatureImpl::class.java.name)
    }

    override fun getMonthlyAccountStats(
        accountId: Long,
        year: Int,
        token: String
    ): Result<MonthlyAccountStatsOutput> = session.authenticate(token) { userId ->
        LOGGER.info("Fetching monthly stats for account $accountId and year $year")

        val booklet = bookletRepository.findAccountByIdWithTransactions(accountId)
            ?: return@authenticate failure(
                ResultState.BOOKLET_NOT_FOUND,
                "Le compte $accountId est introuvable"
            )

        if (booklet.owner?.id != userId) {
            return@authenticate failure(
                ResultState.FORBIDDEN,
                "Vous n'avez pas accès à ce compte"
            )
        }

        val monthlyData = monthlyStatsCalculator.calculateMonthlyStats(
            transactions = booklet.transactions,
            year = year
        )

        LOGGER.info("Monthly stats calculated: ${monthlyData.size} months processed")

        success(
            MonthlyAccountStatsOutput(
                accountId = accountId,
                accountLabel = booklet.label,
                year = year,
                monthlyData = monthlyData
            )
        )
    }

    override fun getCategoryDistribution(
        token: String
    ): Result<CategoryDistributionOutput> = session.authenticate(token) { userId ->
        LOGGER.info("Fetching category distribution for user $userId")

        val booklets = bookletRepository.findBookletsForUser(userId)

        val allTransactions = booklets.flatMap { booklet ->
            booklet.transactions
        }

        LOGGER.info("All transactions fetched: ${allTransactions.size} transactions found")

        val (categories, totalExpenses) = categoryDistributionCalculator.calculateDistribution(
            transactions = allTransactions
        )

        LOGGER.info("Category distribution calculated: ${categories.size} categories found")

        success(
            CategoryDistributionOutput(
                categories = categories,
                totalExpenses = totalExpenses
            )
        )
    }

    override fun getTrendStats(
        token: String
    ): Result<TrendStatsOutput> = session.authenticate(token) { userId ->
        LOGGER.info("Fetching trend stats for user $userId")

        val booklets = bookletRepository.findBookletsForUser(userId)

        val monthlyTrends = trendCalculator.calculateTrend(
            booklets = booklets
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

    override fun getPrevisionalTransactions(
        token: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<PrevisionalTransactionsOutput> = session.authenticate(token) { userId ->
        LOGGER.info("Fetching previsional transactions from $startDate to $endDate for user $userId")

        if (startDate.isAfter(endDate)) {
            return@authenticate failure(
                ResultState.INVALID,
                "La date de début doit être antérieure à la date de fin"
            )
        }

        val booklets = bookletRepository.findBookletsForUser(userId)

        val result = previsionalTransactionFilter.filterPrevisionalTransactions(
            booklets = booklets,
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
                groupedByAccount = result.groupedByAccount,
                totalAmount = result.totalAmount,
                totalIncome = result.totalIncome,
                totalExpenses = result.totalExpenses,
                startDate = startDate,
                endDate = endDate
            )
        )
    }
}