package fr.sacane.jmanager.domain.port.input.featureflag

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.FeatureFlag
import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.port.output.FeatureFlagRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success

class GetAllFeatureFlagsQuery : Query<List<FeatureFlag>>

@Port(Side.APPLICATION)
interface GetAllFeatureFlagsUseCase : QueryHandler<GetAllFeatureFlagsQuery, List<FeatureFlag>> {
    override val queryClass get() = GetAllFeatureFlagsQuery::class
}

@DomainService
class GetAllFeatureFlagsService(
    private val repository: FeatureFlagRepository,
) : GetAllFeatureFlagsUseCase {
    override fun handle(query: GetAllFeatureFlagsQuery): Result<List<FeatureFlag>> {
        val persisted = repository.findAll().associateBy { it.key }
        val flags = FeatureKey.entries.map { key ->
            persisted[key] ?: FeatureFlag(key, enabled = false)
        }
        return success(flags)
    }
}
