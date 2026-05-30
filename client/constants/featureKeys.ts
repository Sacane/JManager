/**
 * Typed registry of all known feature flag keys.
 * Must stay in sync with the backend FeatureKey enum.
 */
export const FEATURE_KEYS = {
  USER_REGISTRATION: 'USER_REGISTRATION',
  EMAIL_VERIFICATION: 'EMAIL_VERIFICATION',
  EMAIL_VERIFICATION_SIMPLE_USER_REGISTRATION: 'EMAIL_VERIFICATION_SIMPLE_USER_REGISTRATION',
} as const

export type FeatureKey = typeof FEATURE_KEYS[keyof typeof FEATURE_KEYS]

/** Human-readable labels for each feature flag key (French UI). */
export const FEATURE_KEY_LABELS: Record<FeatureKey, string> = {
  USER_REGISTRATION: 'Inscription des utilisateurs',
  EMAIL_VERIFICATION: 'Vérification d\'email',
  EMAIL_VERIFICATION_SIMPLE_USER_REGISTRATION: 'Vérification email à l\'inscription (utilisateurs simples)',
}
