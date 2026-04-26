package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.CategoryDistributionOutput
import fr.sacane.jmanager.domain.models.DailyTrendStatsOutput
import fr.sacane.jmanager.domain.models.MonthlyBookletStatsOutput
import fr.sacane.jmanager.domain.models.PrevisionalTransactionsOutput
import fr.sacane.jmanager.domain.models.TrendStatsOutput
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.utils.Result
import java.time.LocalDate
import java.util.UUID

@Deprecated("Use individual use case interfaces from domain.port.input.stats instead")
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

