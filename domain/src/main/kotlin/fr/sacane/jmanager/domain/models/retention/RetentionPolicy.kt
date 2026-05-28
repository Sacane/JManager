package fr.sacane.jmanager.domain.models.retention

/**
 * Strongly-typed duration wrapper for retention thresholds.
 * Enforces positive values at construction time.
 */
@JvmInline
value class RetentionDays(val value: Long) {
    init {
        require(value > 0) { "RetentionDays must be positive, got $value" }
    }
}

/**
 * Domain model expressing the active data-retention rules.
 *
 * Each field corresponds to one category of personal data and its maximum
 * retention period. Additional rules can be added here as new GDPR obligations arise.
 */
data class RetentionPolicy(
    /** Maximum age of an account whose user has never accepted the Terms of Service. */
    val unconsentedAccountRetentionDays: RetentionDays,
)

/**
 * Result returned by [fr.sacane.jmanager.domain.port.input.retention.PurgeExpiredDataUseCase]
 * after a retention run.
 */
data class PurgeSummary(val deletedCount: Int)
