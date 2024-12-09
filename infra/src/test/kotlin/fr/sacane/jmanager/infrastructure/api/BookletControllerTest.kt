package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.api.SessionFeature
import fr.sacane.jmanager.infrastructure.api.account.UserAccountRequest
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.MessageSource
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class BookletControllerTest(
    @LocalServerPort val port: Int,
    @Autowired val accountStateAdapter: AccountStateAdapter,
    @Autowired val sessionFeature: SessionFeature,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val userPostgresRepository: UserPostgresRepository
) {

    @Qualifier("messageSource")
    @Autowired
    private lateinit var messageSource: MessageSource
    private lateinit var token: String
    private var user: User? = null

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }
    }

    @BeforeEach
    fun beforeEach() {
        sessionFeature.register("test", "test", "test").onSuccess { user = it }
        sessionFeature.login("test", "test").onSuccess { token = it.token.tokenValue.toString() }
    }

    @AfterEach
    fun tearDown() {
        accountStateAdapter.clear()
        sessionFeature.logout(user?.id!!, token.asTokenUUID())
        userPostgresRepository.deleteAll()
    }
    @Nested
    inner class BookingBookletTest {
        @Test
        fun `Should create an account with its label and amount then send 200`() {
            val body = UserAccountRequest(user?.id!!.id!!, "test", 1000.toDouble(), "€")
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

        @Test
        fun `Should send 400 with bad currency request`() {
            val body = UserAccountRequest(user?.id!!.id!!, "test", 1000.toDouble(), "ERR")
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/account")
            } Then {
                statusCode(400)
            }
        }
    }

    @Nested
    inner class FindAccountByIdTest {
        @Test
        fun `Request for an account with its id should send 200 with the account in the body`() {
            accountStateAdapter.init(listOf(Account(id = null, amount = 100.toAmount(), labelAccount = "test", owner = user)))
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/account/${user!!.id.id}")
            } Then {
                statusCode(200)
            }
        }

        @Test
        fun `user that does not exists asking for an account should send 401`() {
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/account/${100219}")
            } Then {
                statusCode(401)
            }
        }
    }
}