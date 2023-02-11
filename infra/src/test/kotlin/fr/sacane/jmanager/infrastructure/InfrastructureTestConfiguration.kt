package fr.sacane.jmanager.infrastructure

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
@TestConfiguration
@Profile("test")
class InfrastructureTestConfiguration {
    @Bean
    fun dataSource() = EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.H2)
        .build()
}