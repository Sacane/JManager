package fr.sacane.jmanager.infrastructure

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.port.output.TokenGenerator
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.JwtTokenGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import java.util.*

@SpringBootApplication
class InfrastructureTestApplication

@Configuration
@ComponentScan(
    basePackages = ["fr.sacane.jmanager.domain", "fr.sacane.jmanager.infrastructure"],
    includeFilters = [ComponentScan.Filter(type = FilterType.ANNOTATION, value = [DomainService::class, UseCase::class])]
)
class InfrastructureFullContextTestConfiguration {
    @Bean
    fun tokenGenerator(@Value("\${auth.secret}") secret: String): TokenGenerator {
        val encodedSecret = Base64.getEncoder().encodeToString(secret.toByteArray())
        return JwtTokenGenerator(encodedSecret)
    }
}
