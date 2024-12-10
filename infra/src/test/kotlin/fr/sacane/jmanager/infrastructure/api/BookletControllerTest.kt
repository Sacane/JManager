package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.api.AccountFeature
import fr.sacane.jmanager.domain.port.api.SessionFeature
import fr.sacane.jmanager.infrastructure.api.account.UserAccountRequest
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
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
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class BookletControllerTest(
    @LocalServerPort val port: Int,
    @Autowired val accountStateAdapter: AccountStateAdapter,
    @Autowired val sessionFeature: SessionFeature,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val userPostgresRepository: UserPostgresRepository,
    @Autowired val accountFeature: AccountFeature,
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
    inner class FindAccountByUserIdTest {
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

        @Test
        fun `Request for an account that does not exists must send 404`() {
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/account/${user!!.id.id!!}/unknown")
            } Then {
                statusCode(404)
            }
        }
        @Test
        fun `Request for an existing account should return it and send 200`() {
           accountStateAdapter.init(
               listOf(
                   Account(
                       id = null,
                       amount = Amount.fromString("100.00"),
                       labelAccount = "test",
                       owner = user,
                   )
               )
           )
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/account/${user!!.id.id!!}/test")
            } Then {
                statusCode(200)
                body(
                    "amount", equalTo(100.0F),
                    "labelAccount", equalTo("test"),
                    "id", not(equalTo(null)),
                )
            }
        }
    }
}