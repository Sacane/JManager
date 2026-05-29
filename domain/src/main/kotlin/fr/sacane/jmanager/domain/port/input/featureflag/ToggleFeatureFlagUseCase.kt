package fr.sacane.jmanager.domain.port.input.featureflag

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.FeatureFlag
import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.FeatureFlagRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success

data class ToggleFeatureFlagCommand(val key: FeatureKey, val enabled: Boolean) : Command<FeatureFlag>

@Port(Side.APPLICATION)
interface ToggleFeatureFlagUseCase : CommandHandler<ToggleFeatureFlagCommand, FeatureFlag> {
    override val commandClass get() = ToggleFeatureFlagCommand::class
}

@DomainService
class ToggleFeatureFlagService(
    private val repository: FeatureFlagRepository,
) : ToggleFeatureFlagUseCase {
    override fun handle(command: ToggleFeatureFlagCommand): Result<FeatureFlag> =
        success(repository.upsert(FeatureFlag(command.key, command.enabled)))
}
