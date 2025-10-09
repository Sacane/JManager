package fr.sacane.jmanager.infrastructure.configuration

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.infrastructure.spi.adapters.JwtTokenGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import java.util.*

@Configuration
@ComponentScan(
    basePackages = ["fr.sacane.jmanager.domain", "fr.sacane.jmanager.domain.port", "fr.sacane.jmanager.infrastructure"],
    includeFilters = [ComponentScan.Filter(type = FilterType.ANNOTATION, value = [DomainService::class, UseCase::class])]
)
class HexagonInjectionConfiguration {

    @Bean
    fun tokenGenerator(@Value("\${auth.secret}") secret: String): TokenGenerator {
        val encodedSecret = Base64.getEncoder().encodeToString(secret.toByteArray())
        return JwtTokenGenerator(encodedSecret)
    }
}