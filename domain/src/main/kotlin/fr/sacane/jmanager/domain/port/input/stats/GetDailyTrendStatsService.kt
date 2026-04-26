package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.DailyTrendStatsOutput
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.usecase.DailyTrendCalculator
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.util.logging.Logger

@DomainService
class GetDailyTrendStatsService(
    private val session: SessionManager,
    private val bookletRepository: BookletRepository,
    private val dailyTrendCalculator: DailyTrendCalculator
) : GetDailyTrendStatsUseCase {

    companion object {
        private val LOGGER = Logger.getLogger(GetDailyTrendStatsService::class.java.name)
    }

    override fun handle(query: GetDailyTrendStatsQuery): Result<DailyTrendStatsOutput> = session.authenticate(query.token) { userId ->
        LOGGER.info("Fetching daily trend stats from ${query.startDate} to ${query.endDate} for user $userId")

        if (query.startDate.isAfter(query.endDate)) {
            return@authenticate statsDomainFailure(
                ResultState.INVALID,
                "La date de début doit être antérieure à la date de fin",
                "domain.stats.daily_trend.invalid_date_range"
            )
        }

        withScopedBooklets(bookletRepository, userId, query.bookletId) { scopedBooklets ->
            val dailyTrends = dailyTrendCalculator.calculateDailyTrend(
                booklets = scopedBooklets,
                startDate = query.startDate,
                endDate = query.endDate
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
