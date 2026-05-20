package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.DailyTrendStatsOutput
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.usecase.DailyTrendCalculator
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.logging.Logger

private fun List<Booklet>.aggregateBalanceAt(date: LocalDate): Amount =
    fold(Amount(BigDecimal.ZERO)) { acc, booklet -> acc + booklet.balanceAt(date) }

data class GetDailyTrendStatsQuery(
    val userId: UserId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val bookletId: UUID? = null
) : Query<DailyTrendStatsOutput>

@Port(Side.APPLICATION)
interface GetDailyTrendStatsUseCase : QueryHandler<GetDailyTrendStatsQuery, DailyTrendStatsOutput> {
    override val queryClass get() = GetDailyTrendStatsQuery::class
}

@DomainService
class GetDailyTrendStatsService(
    private val bookletRepository: BookletRepository,
    private val dailyTrendCalculator: DailyTrendCalculator
) : GetDailyTrendStatsUseCase {

    companion object {
        private val LOGGER = Logger.getLogger(GetDailyTrendStatsService::class.java.name)
    }

    override fun handle(query: GetDailyTrendStatsQuery): Result<DailyTrendStatsOutput> {
        val userId = query.userId
        LOGGER.info("Fetching daily trend stats from ${query.startDate} to ${query.endDate} for user $userId")

        if (query.startDate.isAfter(query.endDate)) {
            return statsDomainFailure(
                ResultState.INVALID,
                "La date de début doit être antérieure à la date de fin",
                "domain.stats.daily_trend.invalid_date_range"
            )
        }

        return withScopedBooklets(bookletRepository, userId, query.bookletId) { scopedBooklets ->
            val startingBalance = scopedBooklets.aggregateBalanceAt(query.startDate)
            val dailyTrends = dailyTrendCalculator.calculateDailyTrend(
                booklets = scopedBooklets,
                startDate = query.startDate,
                endDate = query.endDate,
                startingBalance = startingBalance
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
