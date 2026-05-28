package fr.sacane.jmanager.domain.port.output

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.FeatureFlag
import fr.sacane.jmanager.domain.models.FeatureKey

@Port(Side.INFRASTRUCTURE)
interface FeatureFlagRepository {
    fun findByKey(key: FeatureKey): FeatureFlag?
    fun findAll(): List<FeatureFlag>
    fun upsert(flag: FeatureFlag): FeatureFlag
}
