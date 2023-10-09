package fr.sacane.jmanager.infrastructure.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import javax.sql.DataSource

@Configuration
class DatasourceConfig {
    @Bean
    fun datasource(): DataSource {
        val source = DriverManagerDataSource()
        source.setDriverClassName("org.postgresql.Driver")
        source.url = "jdbc:postgresql://localhost:32768/"
        source.username = "postgres"
        source.password = "postgrespw"
        return source
    }

    @Bean
    fun namedParameterJdbcTemplate(): NamedParameterJdbcTemplate {
        return NamedParameterJdbcTemplate(datasource())
    }
}