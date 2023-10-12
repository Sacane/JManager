package fr.sacane.jmanager.infrastructure.configuration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import javax.sql.DataSource

@Configuration
class DatasourceConfig {

    @Autowired
    private lateinit var environment: Environment

    @Bean
    fun datasource(): DataSource {
        val source = DriverManagerDataSource()
        source.setDriverClassName(environment.getProperty("spring.datasource.driver-class-name")!!)
        source.url = environment.getProperty("spring.datasource.url")
        source.username = environment.getProperty("spring.datasource.username")
        source.password = environment.getProperty("spring.datasource.password")
        return source
    }

    @Bean
    fun namedParameterJdbcTemplate(): NamedParameterJdbcTemplate {
        return NamedParameterJdbcTemplate(datasource())
    }
}