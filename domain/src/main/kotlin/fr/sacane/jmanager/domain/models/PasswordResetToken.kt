package fr.sacane.jmanager.domain.models

import java.security.MessageDigest
import java.time.LocalDateTime

/**
 * A single-use permission to choose a new password, bound to one user.
 *
 * Only [tokenHash], the SHA-256 digest of the raw token, is ever stored: the raw value exists in
 * memory and in the email. A slow hash would add nothing to a 256-bit random value, and a
 * deterministic digest can be looked up.
 */
data class PasswordResetToken(
    val tokenHash: String,
    val userId: UserId,
    val expiresAt: LocalDateTime,
) {
    fun isExpired(now: LocalDateTime): Boolean = !now.isBefore(expiresAt)

    companion object {
        /** Lower-case hexadecimal SHA-256 digest of [rawToken]. */
        fun digestOf(rawToken: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(rawToken.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
