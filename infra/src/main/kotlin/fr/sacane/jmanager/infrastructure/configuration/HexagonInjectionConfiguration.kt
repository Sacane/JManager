package fr.sacane.jmanager.infrastructure.configuration

import fr.sacane.jmanager.domain.hexadoc.DomainImplementation
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType

@Configuration
@ComponentScan(
    basePackages = ["fr.sacane.jmanager.domain.port"],
    includeFilters = [ComponentScan.Filter(type = FilterType.ANNOTATION, value = [DomainImplementation::class])]
)
class HexagonInjectionConfiguration