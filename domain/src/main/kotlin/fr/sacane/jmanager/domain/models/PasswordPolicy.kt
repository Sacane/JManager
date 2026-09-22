package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure

/** A rule a new password can fail. Its [key] is the stable identifier the client maps to a message. */
enum class PasswordRule(val key: String) {
    TOO_SHORT("too_short"),
    TOO_LONG("too_long"),
    EQUALS_EMAIL("equals_email"),
}

/**
 * The rules every new password must satisfy, whatever the way it is set: registration, password change,
 * forced change, account creation by an administrator.
 *
 * Applied when a password is **chosen**, never when one is checked at sign-in: existing passwords keep
 * working. Length is counted in characters (code points), not UTF-16 units, so an emoji counts as one.
 * No composition rule is imposed — length is what resists guessing.
 */
object PasswordPolicy {
    const val MIN_LENGTH = 12
    const val MAX_LENGTH = 100

    /** Every rule [password] fails for an account whose address is [email], in declaration order. */
    fun unmetRules(password: String, email: String?): List<PasswordRule> {
        val length = password.codePointCount(0, password.length)
        return buildList {
            if (length < MIN_LENGTH) add(PasswordRule.TOO_SHORT)
            if (length > MAX_LENGTH) add(PasswordRule.TOO_LONG)
            if (email != null && password.equals(email, ignoreCase = true)) add(PasswordRule.EQUALS_EMAIL)
        }
    }

    /**
     * The failure to return when [password] breaks the policy for an account whose address is [email], or
     * `null` when it satisfies it. The failure lists every unmet rule in [DomainError.reasons].
     */
    fun <T> violationOf(password: String, email: String?): Result<T>? {
        val unmet = unmetRules(password, email)
        if (unmet.isEmpty()) return null
        return failure(
            ResultState.PASSWORD_POLICY_VIOLATION,
            DomainError(
                code = ResultState.PASSWORD_POLICY_VIOLATION.code,
                key = "domain.user.password.policy_violation",
                detail = "Le mot de passe ne respecte pas les règles de sécurité",
                reasons = unmet.map { it.key },
            ),
        )
    }
}
