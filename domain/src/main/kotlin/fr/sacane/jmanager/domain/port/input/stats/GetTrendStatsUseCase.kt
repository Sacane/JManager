package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.TrendStatsOutput
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.usecase.TrendCalculator
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.logging.Logger

data class GetTrendStatsQuery(
    val userId: UserId,
    val bookletId: UUID? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
) : Query<TrendStatsOutput>

@Port(Side.APPLICATION)
interface GetTrendStatsUseCase : QueryHandler<GetTrendStatsQuery, TrendStatsOutput> {
    override val queryClass get() = GetTrendStatsQuery::class
}

@DomainService
class GetTrendStatsService(
    private val bookletRepository: BookletRepository,
    private val trendCalculator: TrendCalculator
) : GetTrendStatsUseCase {

    companion object {
        private val LOGGER = Logger.getLogger(GetTrendStatsService::class.java.name)
    }

    override fun handle(query: GetTrendStatsQuery): Result<TrendStatsOutput> {
        val userId = query.userId
        LOGGER.info("Fetching trend stats for user $userId")

        validateDateRange(query.startDate, query.endDate, "domain.stats.trend")?.let { (detail, key) ->
            return statsDomainFailure(ResultState.INVALID, detail, key)
        }

        return withScopedBooklets(bookletRepository, userId, query.bookletId) { scopedBooklets ->
            val monthlyTrends = trendCalculator.calculateTrend(
                booklets = scopedBooklets,
                startDate = query.startDate,
                endDate = query.endDate
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
}
