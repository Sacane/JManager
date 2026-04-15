package fr.sacane.jmanager.infrastructure.datasource

import fr.sacane.jmanager.infrastructure.spi.entity.BookletResource
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.repositories.BookletJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal


@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class InfraUserTest {

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
    lateinit var userPostgresRepository: UserPostgresRepository

    @Autowired
    lateinit var bookletJpaRepository: BookletJpaRepository

    fun basicUserTest(): UserResource {
        return UserResource("johan_test", "0101012000","johan.ramaroson@test.com")
    }
    @AfterEach
    fun clear(){
        userPostgresRepository.deleteByUsername("johan_test")
    }

    @Test
    @Order(1)
    fun `users should correctly be implement into database`(){
        val passwordUser = "01012000"
        val user = UserResource("johan_test", passwordUser,"johan.ramaroson@test.com",  mutableListOf(), mutableListOf())
        userPostgresRepository.save(user)
        val byName = userPostgresRepository.findByUsername("johan_test")
        assertThat(byName).isNotNull
        assertThat(byName!!.username).isEqualTo(user.username)
        val password = byName.password
        assertThat(passwordUser).isEqualTo(password)

    }

    @Test
    @Order(2)
    fun `users should got booklets while add them into database`(){
        val user = basicUserTest()
        user.booklets = mutableListOf()
        userPostgresRepository.save(user)

        val byName = userPostgresRepository.findByUsername(user.username)
        val bookletResource = BookletResource(label = "test booklet")
        bookletResource.amount = BigDecimal(102)

        byName?.booklets!!.add(bookletResource)

        userPostgresRepository.save(byName)

        assertThat(userPostgresRepository.count()).isLessThan(2)
        assertThat(byName.booklets).isNotEmpty
        assertThat(bookletJpaRepository.count()).isGreaterThan(0)
        userPostgresRepository.deleteByUsername("johan_test")

    }

}
