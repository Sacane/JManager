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
 * Domain service that encapsulates the "issue a verification token and notify the user" operation.
 * Used by both registration and resend flows to avoid duplicating token-lifecycle logic.
 * Instantiated explicitly by EmailVerificationConfiguration (not via component scan) because it
 * requires Clock and Duration which are not default Spring beans.
 */
class EmailVerificationIssuer(
    private val tokenRepository: EmailVerificationTokenRepository,
    private val secureTokenGenerator: SecureTokenGenerator,
    private val notificationPort: NotificationPort,
    private val clock: Clock,
    private val tokenTtl: Duration = Duration.ofHours(24),
) {
    fun issue(userId: UserId, email: String): EmailVerificationToken {
        tokenRepository.deleteByUserId(userId)
        val rawToken = secureTokenGenerator.generate()
        val token = EmailVerificationToken(
            token = rawToken,
            userId = userId,
            expiresAt = LocalDateTime.now(clock).plus(tokenTtl),
        )
        tokenRepository.save(token)
        notificationPort.sendVerificationEmail(email, rawToken)
        return token
    }
}
