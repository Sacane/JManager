package fr.sacane.jmanager.infrastructure.spi.configuration

import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.infrastructure.spi.entity.FeatureFlagEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.FeatureFlagJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

/**
 * Synchronises the `feature_flag` table with the known [FeatureKey] enum on every deploy.
 *
 * - Keys present in the enum but absent from the DB are inserted as **disabled** (safe-by-default).
 * - Keys present in the DB but removed from the enum are **deleted** (orphan cleanup).
 * - Existing rows whose key is still in the enum are never touched, preserving admin-configured values.
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
        val knownKeys = FeatureKey.entries.map { it.name }.toSet()
        val existingKeys = jpaRepository.findAll().map { it.key }.toSet()

        val toDelete = existingKeys - knownKeys
        val toInsert = knownKeys - existingKeys

        if (toDelete.isNotEmpty()) {
            jpaRepository.deleteAllById(toDelete)
        }
        toInsert.forEach { key ->
            jpaRepository.save(FeatureFlagEntity(key = key, enabled = false))
        }

        log.info(
            "Feature flag seeding complete: ${toInsert.size} inserted, ${toDelete.size} removed" +
                " (${knownKeys.size} total known)"
        )
    }
}
