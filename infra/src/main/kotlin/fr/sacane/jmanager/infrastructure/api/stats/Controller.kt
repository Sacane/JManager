package fr.sacane.jmanager.infrastructure.api.stats

import fr.sacane.jmanager.domain.port.api.StatsFeature
import fr.sacane.jmanager.domain.port.api.TagFeature
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.infrastructure.api.NotFoundException
import fr.sacane.jmanager.infrastructure.api.currentUser
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
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
    private val statsFeature: StatsFeature,
    private val tagFeature: TagFeature
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

        return statsFeature.getMonthlyBookletStats(bookletId.toUUID(), year, SessionToken(currentUser.token))
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

        return statsFeature.getCategoryDistribution(
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

        return statsFeature.getTrendStats(
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
        val defaultTag = tagFeature.defaultTag(SessionToken(currentUser.token)).mapNotNullOrFailure() ?: throw NotFoundException(
            ResultState.TAG_NOT_FOUND.code, "Default tag not found")
        return statsFeature.getPrevisionalTransactions(SessionToken(currentUser.token), startDate, endDate, bookletId)
            .map {
                it.toDTO(defaultTag)
            }
            .toHttpResponse()
    }
}