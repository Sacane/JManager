package fr.sacane.jmanager.infrastructure.configuration

import fr.sacane.jmanager.domain.hexadoc.DomainService
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType

@Configuration
@ComponentScan(
    basePackages = ["fr.sacane.jmanager.domain", "fr.sacane.jmanager.domain.port", "fr.sacane.jmanager.infrastructure"],
    includeFilters = [ComponentScan.Filter(type = FilterType.ANNOTATION, value = [DomainService::class])]
)
class HexagonInjectionConfiguration