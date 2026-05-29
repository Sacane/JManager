package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.models.FeatureFlag
import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.domain.port.output.FeatureFlagRepository

class InMemoryFeatureFlagRepository : FeatureFlagRepository {
    private val store = mutableMapOf<FeatureKey, FeatureFlag>()

    override fun findByKey(key: FeatureKey): FeatureFlag? = store[key]
    override fun findAll(): List<FeatureFlag> = store.values.toList()
    override fun upsert(flag: FeatureFlag): FeatureFlag { store[flag.key] = flag; return flag }

    fun clear() = store.clear()
}
