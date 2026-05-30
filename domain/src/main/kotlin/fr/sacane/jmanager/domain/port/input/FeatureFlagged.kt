package fr.sacane.jmanager.domain.port.input

import fr.sacane.jmanager.domain.models.FeatureKey

/**
 * Implemented by [Command] and [Query] objects whose execution is gated behind a feature flag.
 *
 * The bus infrastructure reads [featureKey] before dispatch. If the flag is absent or disabled,
 * the dispatch returns [fr.sacane.jmanager.domain.utils.ResultState.FEATURE_DISABLED] immediately
 * — the handler is never invoked.
 *
 * Handlers remain completely unaware of feature flags.
 */
interface FeatureFlagged {
    val featureKey: FeatureKey
}
