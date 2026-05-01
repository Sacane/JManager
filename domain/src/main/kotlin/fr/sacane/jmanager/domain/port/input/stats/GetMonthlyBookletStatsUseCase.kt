package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.MonthlyBookletStatsOutput
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.usecase.MonthlyStatsCalculator
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID
import java.util.logging.Logger

data class GetMonthlyBookletStatsQuery(
    val bookletId: UUID,
    val year: Int,
    val userId: UserId
) : Query<MonthlyBookletStatsOutput>

@Port(Side.APPLICATION)
interface GetMonthlyBookletStatsUseCase : QueryHandler<GetMonthlyBookletStatsQuery, MonthlyBookletStatsOutput> {
    override val queryClass get() = GetMonthlyBookletStatsQuery::class
}

@DomainService
class GetMonthlyBookletStatsService(
    private val bookletRepository: BookletRepository,
    private val monthlyStatsCalculator: MonthlyStatsCalculator
) : GetMonthlyBookletStatsUseCase {

    companion object {
        private val LOGGER = Logger.getLogger(GetMonthlyBookletStatsService::class.java.name)
    }

    override fun handle(query: GetMonthlyBookletStatsQuery): Result<MonthlyBookletStatsOutput> {
        val userId = query.userId
        LOGGER.info("Fetching monthly stats for booklet ${query.bookletId} and year ${query.year}")

        val booklet = bookletRepository.findBookletByIdWithTransactions(query.bookletId)
            ?: return statsDomainFailure(
                ResultState.BOOKLET_NOT_FOUND,
                "Le livret ${query.bookletId} est introuvable",
                "domain.stats.monthly.booklet_not_found"
            )

        if (booklet.owner?.id != userId) {
            return statsDomainFailure(
                ResultState.FORBIDDEN,
                "Vous n'avez pas accès à ce livret",
                "domain.stats.monthly.forbidden"
            )
        }

        val monthlyData = monthlyStatsCalculator.calculateMonthlyStats(
            transactions = booklet.transactions,
            year = query.year
        )

        LOGGER.info("Monthly stats calculated: ${monthlyData.size} months processed")

        return success(
            MonthlyBookletStatsOutput(
                bookletId = query.bookletId,
                bookletLabel = booklet.label,
                year = query.year,
                monthlyData = monthlyData
            )
        )
    }
}
