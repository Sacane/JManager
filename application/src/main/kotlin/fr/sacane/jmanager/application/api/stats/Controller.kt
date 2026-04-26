package fr.sacane.jmanager.application.api.stats

import fr.sacane.jmanager.domain.port.input.stats.GetCategoryDistributionUseCase
import fr.sacane.jmanager.domain.port.input.stats.GetDailyTrendStatsUseCase
import fr.sacane.jmanager.domain.port.input.stats.GetMonthlyBookletStatsUseCase
import fr.sacane.jmanager.domain.port.input.stats.GetPrevisionalTransactionsUseCase
import fr.sacane.jmanager.domain.port.input.stats.GetTrendStatsUseCase
import fr.sacane.jmanager.domain.port.input.tag.DefaultTagQuery
import fr.sacane.jmanager.domain.port.input.tag.DefaultTagUseCase
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.application.api.NotFoundException
import fr.sacane.jmanager.application.api.currentUser
import fr.sacane.jmanager.application.api.toHttpResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID
import java.util.logging.Logger

@RestController
@RequestMapping("/api/stats")
@Validated
class StatsController(
    private val getMonthlyBookletStatsUseCase: GetMonthlyBookletStatsUseCase,
    private val getCategoryDistributionUseCase: GetCategoryDistributionUseCase,
    private val getTrendStatsUseCase: GetTrendStatsUseCase,
    private val getPrevisionalTransactionsUseCase: GetPrevisionalTransactionsUseCase,
    private val getDailyTrendStatsUseCase: GetDailyTrendStatsUseCase,
    private val defaultTagUseCase: DefaultTagUseCase
) {
    companion object {
        private val LOGGER: Logger = Logger.getLogger("StatsController")
    }

    @GetMapping("/monthly/{bookletId}/{year}")
    fun getMonthlyBookletStats(
        @PathVariable bookletId: String,
        @PathVariable @Min(1900) @Max(2100) year: Int
    ): ResponseEntity<MonthlyBookletStatsDTO> {
        LOGGER.info("Requesting monthly stats for booklet $bookletId and year $year")

        return getMonthlyBookletStatsUseCase.getMonthlyBookletStats(bookletId.toUUID(), year, SessionToken(currentUser.token))
            .map { it.toDTO() }
            .toHttpResponse()
    }

    @GetMapping("/category-distribution")
    fun getCategoryDistribution(
        @RequestParam(required = false) bookletId: UUID?,
        @RequestParam(required = false) startDate: LocalDate?,
        @RequestParam(required = false) endDate: LocalDate?
    ): ResponseEntity<CategoryDistributionDTO> {
        LOGGER.info("Requesting category distribution")

        return getCategoryDistributionUseCase.getCategoryDistribution(
            token = SessionToken(currentUser.token),
            bookletId = bookletId,
            startDate = startDate,
            endDate = endDate
        )
            .map { it.toDTO() }
            .toHttpResponse()
    }

    @GetMapping("/trends")
    fun getTrendStats(
        @RequestParam(required = false) bookletId: UUID?,
        @RequestParam(required = false) startDate: LocalDate?,
        @RequestParam(required = false) endDate: LocalDate?
    ): ResponseEntity<TrendStatsDTO> {
        LOGGER.info("Requesting trend stats")

        return getTrendStatsUseCase.getTrendStats(
            token = SessionToken(currentUser.token),
            bookletId = bookletId,
            startDate = startDate,
            endDate = endDate
        )
            .map { it.toDTO() }
            .toHttpResponse()
    }

    @GetMapping("/previsional")
    fun getPrevisionalTransactions(
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
        @RequestParam(required = false) bookletId: UUID?
    ): ResponseEntity<PrevisionalTransactionsDTO> {
        LOGGER.info("Requesting previsional transactions from $startDate to $endDate")
        val defaultTag = defaultTagUseCase.handle(DefaultTagQuery(SessionToken(currentUser.token))).mapNotNullOrFailure() ?: throw NotFoundException(
            ResultState.TAG_NOT_FOUND.code, "Default tag not found")
        return getPrevisionalTransactionsUseCase.getPrevisionalTransactions(SessionToken(currentUser.token), startDate, endDate, bookletId)
            .map {
                it.toDTO(defaultTag)
            }
            .toHttpResponse()
    }

    @GetMapping("/daily-trends")
    fun getDailyTrendStats(
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
        @RequestParam(required = false) bookletId: UUID?
    ): ResponseEntity<DailyTrendStatsDTO> {
        LOGGER.info("Requesting daily trend stats from $startDate to $endDate")

        return getDailyTrendStatsUseCase.getDailyTrendStats(
            token = SessionToken(currentUser.token),
            startDate = startDate,
            endDate = endDate,
            bookletId = bookletId
        )
            .map { it.toDTO() }
            .toHttpResponse()
    }
}