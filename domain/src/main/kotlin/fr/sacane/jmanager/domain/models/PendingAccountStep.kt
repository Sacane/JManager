package fr.sacane.jmanager.domain.models

/**
 * A step an account must complete before using the application, in the order it must be completed.
 * While one is pending, the account may only complete it, read its own status, sign out or erase itself.
 */
enum class PendingAccountStep(val errorKey: String, val detail: String) {
    CONSENT(
        "domain.user.pending_step.consent_required",
        "Vous devez accepter les conditions d'utilisation et la politique de confidentialité",
    ),
    PASSWORD_CHANGE(
        "domain.user.pending_step.password_change_required",
        "Vous devez remplacer votre mot de passe temporaire",
    ),
}
