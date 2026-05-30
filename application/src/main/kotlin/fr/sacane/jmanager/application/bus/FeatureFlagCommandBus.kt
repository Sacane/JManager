package fr.sacane.jmanager.application.bus

import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.FeatureFlagged
import fr.sacane.jmanager.domain.port.output.FeatureFlagRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Decorator around [LoggingCommandBus] that enforces feature flag gates.
 *
 * If the dispatched command implements [FeatureFlagged], the corresponding flag is checked
 * in [FeatureFlagRepository] before the command reaches the handler:
 * - Flag absent or disabled → [ResultState.FEATURE_DISABLED] is returned immediately.
 * - Flag enabled → dispatch proceeds normally via the delegate.
 *
 * Commands that do not implement [FeatureFlagged] are forwarded without any flag lookup.
 *
 * This keeps handlers completely unaware of feature flags.
 */
@Primary
@Component
class FeatureFlagCommandBus(
    private val delegate: LoggingCommandBus,
    private val featureFlagRepository: FeatureFlagRepository,
) : CommandBus {

    override fun <R> dispatch(command: Command<R>): Result<R> {
        if (command is FeatureFlagged) {
            val flag = featureFlagRepository.findByKey(command.featureKey)
            if (flag == null || !flag.enabled) {
                return failure(
                    ResultState.FEATURE_DISABLED,
                    DomainError(
                        ResultState.FEATURE_DISABLED.code,
                        "feature_flag.feature_disabled",
                        "Cette fonctionnalité est désactivée"
                    )
                )
            }
        }
        return delegate.dispatch(command)
    }
}
