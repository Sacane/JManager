package fr.sacane.jmanager.infrastructure.spi.configuration

import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.infrastructure.spi.entity.FeatureFlagEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.FeatureFlagJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

/**
 * Ensures every known [FeatureKey] has a row in the `feature_flag` table after each deploy.
 * Newly added keys are inserted as **disabled** (safe-by-default).
 * Existing rows — including those toggled ON by an administrator — are never overwritten.
 */
@Component
class FeatureFlagSeeder(
    private val jpaRepository: FeatureFlagJpaRepository,
) : ApplicationListener<ContextRefreshedEvent> {

    private val log = LoggerFactory.getLogger(FeatureFlagSeeder::class.java)
    private var seeded = false

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (seeded) return
        seed()
        seeded = true
    }

    internal fun seed() {
        var inserted = 0
        FeatureKey.entries.forEach { key ->
            if (!jpaRepository.existsById(key.name)) {
                jpaRepository.save(FeatureFlagEntity(key = key.name, enabled = false))
                inserted++
            }
        }
        log.info("Feature flag seeding complete: $inserted new flag(s) inserted (${FeatureKey.entries.size} total known)")
    }
}
