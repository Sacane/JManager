package fr.sacane.jmanager.domain.models

enum class FeatureKey {
    EMAIL_VERIFICATION,
    EMAIL_VERIFICATION_SIMPLE_USER_REGISTRATION,
}

data class FeatureFlag(
    val key: FeatureKey,
    val enabled: Boolean,
)
