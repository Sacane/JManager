package fr.sacane.jmanager.infrastructure.spi.scheduler

import fr.sacane.jmanager.domain.models.retention.RetentionDays
import fr.sacane.jmanager.domain.models.retention.RetentionPolicy
import fr.sacane.jmanager.domain.port.input.retention.PurgeExpiredDataUseCase
import fr.sacane.jmanager.infrastructure.spi.configuration.RetentionProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Infrastructure adapter that drives [PurgeExpiredDataUseCase] on a configurable cron schedule.
 *
 * This component contains **no retention logic** — it is a pure trigger adapter responsible for:
 * - Translating [RetentionProperties] into a domain [RetentionPolicy].
 * - Providing the current server-side timestamp (forbidden inside the domain).
 * - Logging the purge result.
 *
 * Disable in a given environment by setting `jmanager.retention.cron=-`.
 */
@Component
class RetentionScheduler(
    private val purgeExpiredDataUseCase: PurgeExpiredDataUseCase,
    private val retentionProperties: RetentionProperties,
) {
    private val log = LoggerFactory.getLogger(RetentionScheduler::class.java)

    @Scheduled(cron = "\${jmanager.retention.cron:0 0 2 * * *}")
    fun run() {
        val policy = RetentionPolicy(
            unconsentedAccountRetentionDays = RetentionDays(retentionProperties.unconsentedAccountDays),
        )
        val summary = purgeExpiredDataUseCase.purge(policy, LocalDateTime.now())
        log.info("Retention purge completed: ${summary.deletedCount} account(s) deleted")
    }
}
