package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.infrastructure.api.booklet.BookletBookingRequest
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
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class BookletControllerTest(
    @param:LocalServerPort val port: Int,
    @param:Autowired val accountStateAdapter: AccountStateTestAdapter,
    @param:Autowired val objectMapper: ObjectMapper
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
                    Booklet(
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
                    Booklet(
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
                    Booklet(
                        id = null,
                        amount = Amount.fromString("50.00"),
                        labelAccount = "test2",
                        owner = user,
                    ),
                    Booklet(
                        id = null,
                        amount = Amount.fromString("60.00"),
                        labelAccount = "test3",
                        owner = user,
                    ),
                    Booklet(
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

    @Nested
    inner class ReportBookletEndpointTest {

        @Test
        fun `Request report for a booklet with valid month and year should return 200`() {
            accountStateAdapter.init(
                listOf(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("1000.00"),
                        labelAccount = "Compte Épargne",
                        owner = user,
                    )
                )
            )
            val booklet = accountStateAdapter.get().first()
            val currentDate = LocalDate.now()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", currentDate.monthValue)
                queryParam("year", currentDate.year)
            } When {
                get("/api/account/report/${booklet.id}")
            } Then {
                statusCode(200)
                body("label", equalTo("Compte Épargne"))
                body("realSold", equalTo("1000.00"))
            }
        }

        @Test
        fun `Request report for a booklet with transactions should return all transactions`() {
            val booklet = Booklet(
                id = null,
                amount = Amount.fromString("1000.00"),
                labelAccount = "Compte Test",
                owner = user,
            )
            val currentDate = LocalDate.now()
            booklet.addTransaction(
                Transaction(
                    id = null,
                    label = "Transaction 1",
                    amount = Amount.fromString("100.00"),
                    date = currentDate,
                    isPreview = false,
                    isIncome = true,
                )
            )
            booklet.addTransaction(
                Transaction(
                    id = null,
                    label = "Transaction 2",
                    amount = Amount.fromString("50.00"),
                    date = currentDate,
                    isPreview = false,
                    isIncome = false,
                )
            )

            accountStateAdapter.init(listOf(booklet))
            val savedBooklet = accountStateAdapter.get().first()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", currentDate.monthValue)
                queryParam("year", currentDate.year)
            } When {
                get("/api/account/report/${savedBooklet.id}")
            } Then {
                statusCode(200)
                body("label", equalTo("Compte Test"))
                body("transactions.size()", equalTo(2))
            }
        }

        @Test
        fun `Request report for non-existing booklet should return 404`() {
            val currentDate = LocalDate.now()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", currentDate.monthValue)
                queryParam("year", currentDate.year)
            } When {
                get("/api/account/report/9999")
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Request report with invalid month should return 400`() {
            accountStateAdapter.init(
                listOf(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("1000.00"),
                        labelAccount = "Test Account",
                        owner = user,
                    )
                )
            )
            val booklet = accountStateAdapter.get().first()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", 13)
                queryParam("year", 2025)
            } When {
                get("/api/account/report/${booklet.id}")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `Request report with invalid year should handle correctly`() {
            accountStateAdapter.init(
                listOf(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("500.00"),
                        labelAccount = "Test Account",
                        owner = user,
                    )
                )
            )
            val booklet = accountStateAdapter.get().first()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", 1)
                queryParam("year", 2025)
            } When {
                get("/api/account/report/${booklet.id}")
            } Then {
                statusCode(200)
            }
        }

        @Test
        fun `Request report without authentication should return 401 or 403`() {
            val currentDate = LocalDate.now()

            Given {
                port(port)
                header("Content-Type", "application/json")
                queryParam("month", currentDate.monthValue)
                queryParam("year", currentDate.year)
            } When {
                get("/api/account/report/1")
            } Then {
                statusCode(org.hamcrest.Matchers.either(equalTo(401)).or(equalTo(403)))
            }
        }

        @Test
        fun `Request report should return preview and real sold values`() {
            accountStateAdapter.init(
                listOf(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("2000.00"),
                        labelAccount = "Compte Principal",
                        owner = user,
                    )
                )
            )
            val booklet = accountStateAdapter.get().first()
            val currentDate = LocalDate.now()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", currentDate.monthValue)
                queryParam("year", currentDate.year)
            } When {
                get("/api/account/report/${booklet.id}")
            } Then {
                statusCode(200)
                body("realSold", equalTo("2000.00"))
                body("previewSold", org.hamcrest.Matchers.notNullValue())
            }
        }
    }
}