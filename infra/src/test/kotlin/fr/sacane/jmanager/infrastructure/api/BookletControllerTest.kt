package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.infrastructure.api.account.BookletBookingRequest
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.generateCookie
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class BookletControllerTest(
    @LocalServerPort val port: Int,
    @Autowired val accountStateAdapter: AccountStateTestAdapter,
    @Autowired val objectMapper: ObjectMapper
): AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        accountStateAdapter.clear()
    }
    @Nested
    inner class BookingBookletTest {
        @Test
        fun `Should create an account with its label and amount then send 200`() {
            val body = BookletBookingRequest("test", 1000.toDouble(), "€")
            Given {
                port(port)
                cookie("token", token)
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
            val body = BookletBookingRequest("test", 1000.toDouble(), "ERR")
            Given {
                port(port)
                cookie("token", token)
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
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                delete("/api/account/$accountID")
            } Then {
                statusCode(200)
            }
            assertTrue(accountStateAdapter.get().isEmpty())
        }

        @Test
        fun `Delete account from an ID of an account that does not exists should return 404`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                delete("/api/account/231")
            } Then {
                statusCode(404)
            }
        }
        @Test
        fun `Delete account from an ID of an account by a user that does not exists should return 404`() {
            Given {
                port(port)
                cookie(generateCookie(token))
                header("Content-Type", "application/json")
            } When {
                delete("/api/account/100")
            } Then {
                statusCode(404)
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
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/account/${account.id}")
            } Then {
                statusCode(200)
                body(
                    "amount", equalTo("100.00"),
                    "labelAccount", equalTo("test"),
                )
            }
        }

        @Test
        fun `Request for an non registered booklet ID must send 404`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/account/0")
            } Then {
                statusCode(404)
            }
        }
    }
    @Nested
    inner class RequestBookletEndpointTest {

        @Test
        fun `Request for all booklets of a user must return 200 with the asking booklets`() {
            accountStateAdapter.init(
                listOf(
                    Account(
                        id = null,
                        amount = Amount.fromString("50.00"),
                        labelAccount = "test2",
                        owner = user,
                    ),
                    Account(
                        id = null,
                        amount = Amount.fromString("60.00"),
                        labelAccount = "test3",
                        owner = user,
                    ),
                    Account(
                        id = null,
                        amount = Amount.fromString("0.00"),
                        labelAccount = "test",
                        owner = user,
                    )
                )
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/account")
            } Then {
                statusCode(200)
                body("size()", equalTo(3))
            }
        }
    }
}