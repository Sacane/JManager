package fr.sacane.jmanager.application.domain_server

import fr.sacane.jmanager.domain.port.output.Hasher
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class UserDomainServerTest {

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
    }

    @Autowired
    private lateinit var userPostgresRepository: UserPostgresRepository

    @Autowired
    private lateinit var hasher: Hasher

    @AfterEach
    fun deleteUserTest(){
        userPostgresRepository.deleteByUsername("Sacane_test")
    }

    @Test
    fun `User registered has its password check correctly`(){
        val pwd1 = "01012000"
        userPostgresRepository.deleteByUsername("Sacane_test")
        val userEntity = UserResource("Sacane_test", hasher.hash(pwd1), "sacane.test@grostest.fr")
        userPostgresRepository.save(userEntity)
        val getUser = userPostgresRepository.findByUsername("Sacane_test")
        assertThat(getUser).isNotNull
        assertThat(getUser?.username).isEqualTo("Sacane_test")
        assertThat(hasher.verify("01012000", getUser?.password!!)).isTrue
    }
}