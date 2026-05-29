package fr.sacane.jmanager.domain.port.input.featureflag

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.port.output.FeatureFlagRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success

data class IsFeatureEnabledQuery(val key: FeatureKey) : Query<Boolean>

@Port(Side.APPLICATION)
interface IsFeatureEnabledUseCase : QueryHandler<IsFeatureEnabledQuery, Boolean> {
    override val queryClass get() = IsFeatureEnabledQuery::class
}

@DomainService
class IsFeatureEnabledService(
    private val repository: FeatureFlagRepository,
) : IsFeatureEnabledUseCase {
    override fun handle(query: IsFeatureEnabledQuery): Result<Boolean> =
        success(repository.findByKey(query.key)?.enabled ?: false)
}
