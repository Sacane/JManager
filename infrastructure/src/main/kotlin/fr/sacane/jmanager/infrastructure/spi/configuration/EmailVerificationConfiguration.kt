package fr.sacane.jmanager.infrastructure.spi.configuration

import fr.sacane.jmanager.domain.port.output.NotificationPort
import fr.sacane.jmanager.domain.port.output.SecureTokenGenerator
import fr.sacane.jmanager.domain.port.output.repository.EmailVerificationTokenRepository
import fr.sacane.jmanager.domain.usecase.EmailVerificationIssuer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
@EnableConfigurationProperties(EmailVerificationProperties::class)
class EmailVerificationConfiguration {

    @Bean
    fun systemClock(): Clock = Clock.systemUTC()

    @Bean
    fun emailVerificationIssuer(
        tokenRepository: EmailVerificationTokenRepository,
        secureTokenGenerator: SecureTokenGenerator,
        notificationPort: NotificationPort,
        clock: Clock,
        props: EmailVerificationProperties,
    ): EmailVerificationIssuer = EmailVerificationIssuer(
        tokenRepository = tokenRepository,
        secureTokenGenerator = secureTokenGenerator,
        notificationPort = notificationPort,
        clock = clock,
        tokenTtl = props.tokenTtl,
    )
}
