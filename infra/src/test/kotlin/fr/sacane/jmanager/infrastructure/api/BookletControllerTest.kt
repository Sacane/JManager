package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.api.SessionFeature
import fr.sacane.jmanager.infrastructure.api.account.UserAccountDTO
import fr.sacane.jmanager.infrastructure.api.setup.AccountFakeTestAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class BookletControllerTest(
    @LocalServerPort val port: Int,
    @Autowired val accountFakeTestAdapter: AccountFakeTestAdapter,
    @Autowired val sessionFeature: SessionFeature,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val userPostgresRepository: UserPostgresRepository
) {

    private lateinit var token: String
    private var userId: UserId? = null

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }
    }

    @BeforeEach
    fun beforeEach() {
        sessionFeature.register("test", "test", "test").onSuccess { userId = it.id }
        sessionFeature.login("test", "test").onSuccess { token = it.token.tokenValue.toString() }
    }

    @AfterEach
    fun tearDown() {
        accountFakeTestAdapter.clear()
        sessionFeature.logout(userId!!, token.asTokenUUID())
        userPostgresRepository.deleteAll()
    }

    @Test
    fun `Should create an account with its label and amount`() {
        val body = UserAccountDTO(userId?.id!!, "test", 1000.toDouble(), "€")
        Given {
            port(port)
            header("Authorization", token)
            header("Content-Type", "application/json")
            body(objectMapper.writeValueAsString(body))
        } When {
            post("/api/account")
        } Then {
            statusCode(200)
            body("label", equalTo("test"), "amount", equalTo("1000.00"), "currency", equalTo("€"))
        }
    }

    fun `Should `() {

    }
}