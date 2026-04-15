package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.port.api.UserFeature
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName


abstract class AuthenticatedUserTest {
    lateinit var token: String
    lateinit var refreshToken: String
    var user: User? = null

    @Autowired
    private lateinit var userPostgresRepository: UserPostgresRepository

    @Autowired
    private lateinit var session: UserFeature

    @Autowired
    private lateinit var sessionManager: SessionManager

    companion object {
        private val postgresContainer = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("test")
            .withUsername("sa")
            .withPassword("sa")
            .withReuse(true)

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
    }

    @BeforeEach
    fun beforeEach() {
        session.register("test", "test", "test").onSuccess { user = it }
        session.login("test", "test").onSuccess {
            token = it.token
            refreshToken = sessionManager.findSessionByToken(SessionToken(it.token))?.refreshToken?.toString()
                ?: error("Refresh token should be initialized for authenticated test user")
        }
    }

    @AfterEach
    fun tearDown() {
        session.logout(SessionToken(token.asTokenUUID()))
        userPostgresRepository.deleteAll()
    }
}
