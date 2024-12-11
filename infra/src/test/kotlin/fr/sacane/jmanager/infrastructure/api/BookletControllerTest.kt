package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.port.api.SessionFeature
import fr.sacane.jmanager.infrastructure.api.account.UserBookletRequest
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class BookletControllerTest(
    @LocalServerPort val port: Int,
    @Autowired val accountStateAdapter: AccountStateAdapter,
    @Autowired val sessionFeature: SessionFeature,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val userPostgresRepository: UserPostgresRepository,
) {

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
            val body = UserBookletRequest(user?.id!!.value!!, "test", 1000.toDouble(), "€")
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
            val body = UserBookletRequest(user?.id!!.value!!, "test", 1000.toDouble(), "ERR")
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
    inner class DeleteBookletTest {
        @Test
        fun `Request delete account from its ID should return 200`() {
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
            val accountID = accountStateAdapter.get().first().id!!

            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
            } When {
                delete("/api/account/${user!!.id.value!!}/$accountID")
            } Then {
                statusCode(200)
            }
            assertTrue(accountStateAdapter.get().isEmpty())
        }

        @Test
        fun `Delete account from an ID of an account that does not exists should return 404`() {
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
            } When {
                delete("/api/account/${user!!.id.value!!}/100")
            } Then {
                statusCode(404)
            }
        }
        @Test
        fun `Delete account from an ID of an account by a user that does not exists should return 401`() {
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
            } When {
                delete("/api/account/203/100")
            } Then {
                statusCode(401)
            }
        }
    }

    @Nested
    inner class FindByIdBookletEndpointTest {
        @Test
        fun `Request a Booklet from an existing ID must return 200 with the asking booklet`() {
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
            val account = accountStateAdapter.get().first()

            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/account/${account.id}/user/${user?.id?.value}")
            } Then {
                statusCode(200)
                body(
                    "amount", equalTo(100.0F),
                    "labelAccount", equalTo("test"),
                )
            }
        }

        @Test
        fun `Request for an non registered booklet ID must send 404`() {
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/account/user/${user?.id?.value}/find/0")
            } Then {
                statusCode(404)
            }
        }
    }
}