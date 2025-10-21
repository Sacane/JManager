package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.port.api.UserFeature
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.RestAssured
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName


abstract class AuthenticatedUserTest {
    lateinit var token: String
    var user: User? = null

    @Autowired
    private lateinit var userPostgresRepository: UserPostgresRepository

    @Autowired
    private lateinit var session: UserFeature

    companion object {
        private val postgresContainer = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("test")
            .withUsername("sa")
            .withPassword("sa")

        init {
            postgresContainer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.username") { postgresContainer.username }
            registry.add("spring.datasource.password") { postgresContainer.password }
        }

        @BeforeAll
        @JvmStatic
        fun setup() {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }
    }

    @BeforeEach
    fun beforeEach() {
        session.register("test", "test", "test").onSuccess { user = it }
        session.login("test", "test").onSuccess { token = it.token }
    }

    @AfterEach
    fun tearDown() {
        session.logout(token.asTokenUUID())
        userPostgresRepository.deleteAll()
    }
}