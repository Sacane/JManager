package fr.sacane.jmanager.domain.models

enum class FeatureKey {
    USER_REGISTRATION
}

data class FeatureFlag(
    val key: FeatureKey,
    val enabled: Boolean,
)
