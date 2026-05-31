package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.models.EmailVerificationToken
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.NotificationPort
import fr.sacane.jmanager.domain.port.output.SecureTokenGenerator
import fr.sacane.jmanager.domain.port.output.repository.EmailVerificationTokenRepository
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

/**
 * Domain service responsible solely for the verification-token lifecycle:
 * invalidate any existing token for the user, generate a fresh one, persist it, and return it.
 *
 * Notification is intentionally excluded — callers decide which email variant to send
 * (combined welcome+verification for new registrations, standalone for resend).
 *
 * Instantiated explicitly by EmailVerificationConfiguration (not via component scan) because it
 * requires Clock and Duration which are not default Spring beans.
 */
class EmailVerificationIssuer(
    private val tokenRepository: EmailVerificationTokenRepository,
    private val secureTokenGenerator: SecureTokenGenerator,
    private val clock: Clock,
    private val tokenTtl: Duration = Duration.ofHours(24),
) {
    fun issue(userId: UserId): EmailVerificationToken {
        tokenRepository.deleteByUserId(userId)
        val rawToken = secureTokenGenerator.generate()
        val token = EmailVerificationToken(
            token = rawToken,
            userId = userId,
            expiresAt = LocalDateTime.now(clock).plus(tokenTtl),
        )
        tokenRepository.save(token)
        return token
    }
}
