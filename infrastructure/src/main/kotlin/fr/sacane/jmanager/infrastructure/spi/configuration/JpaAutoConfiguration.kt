package fr.sacane.jmanager.infrastructure.spi.configuration

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EntityScan(basePackages = ["fr.sacane.jmanager.infrastructure.spi.entity"])
@EnableJpaRepositories(basePackages = ["fr.sacane.jmanager.infrastructure.spi"])
class JpaAutoConfiguration
