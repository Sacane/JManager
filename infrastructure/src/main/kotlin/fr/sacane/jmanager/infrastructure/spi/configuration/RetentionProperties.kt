package fr.sacane.jmanager.infrastructure.spi.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Typed configuration for the GDPR data-retention policy.
 *
 * Bound from the `jmanager.retention.*` namespace. Consumed by [RetentionScheduler]
 * to build a [fr.sacane.jmanager.domain.models.retention.RetentionPolicy] at runtime.
 *
 * @property unconsentedAccountDays days after which an unconsented account is eligible for purge
 * @property cron                   Spring cron expression for the purge job; use "-" to disable
 */
@ConfigurationProperties(prefix = "jmanager.retention")
data class RetentionProperties(
    val unconsentedAccountDays: Long = 30L,
    val cron: String = "0 0 2 * * *",
)
