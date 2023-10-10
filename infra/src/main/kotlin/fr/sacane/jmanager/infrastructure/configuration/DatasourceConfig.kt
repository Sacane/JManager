package fr.sacane.jmanager.infrastructure.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.util.logging.Logger
import javax.sql.DataSource

@Configuration
class DatasourceConfig {

    @Value("spring.datasource.url")
    private lateinit var url: String

    @Bean
    fun datasource(): DataSource {
        logger.info("Url generated : $url")
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
    private val logger: Logger = Logger.getLogger(DatasourceConfig::class.simpleName)
}