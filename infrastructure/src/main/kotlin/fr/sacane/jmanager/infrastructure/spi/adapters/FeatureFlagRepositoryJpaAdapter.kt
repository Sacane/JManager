package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.FeatureFlag
import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.domain.port.output.FeatureFlagRepository
import fr.sacane.jmanager.infrastructure.spi.entity.FeatureFlagEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.FeatureFlagJpaRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@Adapter(Side.INFRASTRUCTURE)
class FeatureFlagRepositoryJpaAdapter(
    private val jpaRepository: FeatureFlagJpaRepository,
) : FeatureFlagRepository {

    override fun findByKey(key: FeatureKey): FeatureFlag? =
        jpaRepository.findById(key.name).orElse(null)?.toDomain()

    @Cacheable("featureFlags")
    override fun findAll(): List<FeatureFlag> =
        jpaRepository.findAll().mapNotNull { it.toDomain() }

    @CacheEvict("featureFlags", allEntries = true)
    override fun upsert(flag: FeatureFlag): FeatureFlag {
        val entity = jpaRepository.findById(flag.key.name)
            .orElse(FeatureFlagEntity(key = flag.key.name))
        entity.enabled = flag.enabled
        return jpaRepository.save(entity).toDomain()!!
    }
}

private fun FeatureFlagEntity.toDomain(): FeatureFlag? {
    val key = FeatureKey.entries.firstOrNull { it.name == key } ?: return null
    return FeatureFlag(key, enabled)
}
