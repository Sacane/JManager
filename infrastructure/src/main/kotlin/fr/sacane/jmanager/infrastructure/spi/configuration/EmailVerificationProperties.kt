package fr.sacane.jmanager.infrastructure.spi.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Typed configuration for the email verification flow.
 *
 * Bound from the `app.email-verification.*` namespace.
 *
 * @property tokenTtl lifetime of a verification token (ISO-8601 duration, e.g. PT24H)
 */
@ConfigurationProperties(prefix = "app.email-verification")
data class EmailVerificationProperties(
    val tokenTtl: Duration = Duration.ofHours(24),
)
