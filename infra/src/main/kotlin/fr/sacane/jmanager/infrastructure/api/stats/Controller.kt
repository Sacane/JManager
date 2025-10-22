package fr.sacane.jmanager.infrastructure.api.stats

import fr.sacane.jmanager.domain.port.api.StatsFeature
import fr.sacane.jmanager.domain.port.api.TagFeature
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.infrastructure.api.NotFoundException
import fr.sacane.jmanager.infrastructure.api.currentUser
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.logging.Logger

@RestController
@RequestMapping("/api/stats")
class StatsController(
    private val statsFeature: StatsFeature,
    private val tagFeature: TagFeature
) {
    companion object {
        private val LOGGER: Logger = Logger.getLogger("StatsController")
    }

    @GetMapping("/monthly/{accountId}/{year}")
    fun getMonthlyAccountStats(
        @PathVariable accountId: String,
        @PathVariable year: Int
    ): ResponseEntity<MonthlyAccountStatsDTO> {
        LOGGER.info("Requesting monthly stats for account $accountId and year $year")

        return statsFeature.getMonthlyAccountStats(accountId.toUUID(), year, currentUser.token)
            .map { it.toDTO() }
            .toHttpResponse()
    }

    @GetMapping("/category-distribution")
    fun getCategoryDistribution(): ResponseEntity<CategoryDistributionDTO> {
        LOGGER.info("Requesting category distribution")

        return statsFeature.getCategoryDistribution(currentUser.token)
            .map { it.toDTO() }
            .toHttpResponse()
    }

    @GetMapping("/trends")
    fun getTrendStats(): ResponseEntity<TrendStatsDTO> {
        LOGGER.info("Requesting trend stats")

        return statsFeature.getTrendStats(currentUser.token)
            .map { it.toDTO() }
            .toHttpResponse()
    }

    @GetMapping("/previsional")
    fun getPrevisionalTransactions(
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate
    ): ResponseEntity<PrevisionalTransactionsDTO> {
        LOGGER.info("Requesting previsional transactions from $startDate to $endDate")
        val defaultTag = tagFeature.defaultTag(currentUser.token).mapNotNullOrFailure() ?: throw NotFoundException(
            ResultState.TAG_NOT_FOUND.code, "Default tag not found")
        return statsFeature.getPrevisionalTransactions(currentUser.token, startDate, endDate)
            .map {
                it.toDTO(defaultTag)
            }
            .toHttpResponse()
    }
}