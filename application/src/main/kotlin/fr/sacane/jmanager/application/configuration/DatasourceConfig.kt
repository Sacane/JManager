package fr.sacane.jmanager.application.configuration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import javax.sql.DataSource

@Configuration
class DatasourceConfig {

    @Autowired
    private lateinit var environment: Environment

    @Bean
    fun datasource(): DataSource {
        val config = HikariConfig().apply {
            driverClassName = environment.getProperty("spring.datasource.driver-class-name")
            jdbcUrl = environment.getProperty("spring.datasource.url")
            username = environment.getProperty("spring.datasource.username")
            password = environment.getProperty("spring.datasource.password")
            maximumPoolSize = environment.getProperty("spring.datasource.hikari.maximum-pool-size", Int::class.java, 10)
            minimumIdle = environment.getProperty("spring.datasource.hikari.minimum-idle", Int::class.java, 2)
            connectionTimeout = environment.getProperty("spring.datasource.hikari.connection-timeout", Long::class.java, 30000L)
            idleTimeout = environment.getProperty("spring.datasource.hikari.idle-timeout", Long::class.java, 600000L)
            leakDetectionThreshold = environment.getProperty("spring.datasource.hikari.leak-detection-threshold", Long::class.java, 60000L)
        }
        return HikariDataSource(config)
    }

    @Bean
    fun namedParameterJdbcTemplate(): NamedParameterJdbcTemplate {
        return NamedParameterJdbcTemplate(datasource())
    }

}
