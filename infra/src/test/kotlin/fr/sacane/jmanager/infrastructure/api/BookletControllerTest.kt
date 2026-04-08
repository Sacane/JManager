package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.infrastructure.api.booklet.BookletBookingRequest
import fr.sacane.jmanager.infrastructure.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.BookletTransaction
import fr.sacane.jmanager.infrastructure.api.setup.TransactionStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.BookletRegularTransactionInput
import fr.sacane.jmanager.infrastructure.api.setup.RegularTrackerStateRepository
import fr.sacane.jmanager.infrastructure.api.setup.RegularTransactionStateForTestAdapter
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import fr.sacane.jmanager.infrastructure.generateCookie
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import java.time.YearMonth
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
    @param:Autowired val bookletStateAdapter: BookletStateTestAdapter,
    @param:Autowired val objectMapper: ObjectMapper,
    @param:Autowired private val transactionStateTestAdapter: TransactionStateTestAdapter,
    @param:Autowired private val regularTransactionStateAdapter: RegularTransactionStateForTestAdapter
): AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        regularTransactionStateAdapter.clear()
        bookletStateAdapter.clear()
    }
    @Nested
    inner class BookingBookletTest {
        @Test
        fun `Should create a booklet with its label and amount then send 200`() {
            val body = BookletBookingRequest("test", 1000.toDouble(), "€")
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/booklet")
            } Then {
                statusCode(200)
                body("label", equalTo("test"), "amount", equalTo("1000.00"), "currency", equalTo("€"))
            }
        }
        @Test
        fun `Should return invalid request 400 when trying to save more than 6 booklets`() {
            val body = BookletBookingRequest("test7", 1000.toDouble(), "€")
            val booklets = mutableListOf<Booklet>()
            repeat(6) {
                booklets.add(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("100.00"),
                        label = "test$it",
                        owner = user,
                    )
                )
            }
            bookletStateAdapter.init(booklets)
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/booklet")
            } Then {
                statusCode(400)
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
                post("/api/booklet")
            } Then {
                statusCode(400)
            }
        }
    }

    @Nested
    inner class DeleteBookletTest {
        @Test
        fun `Request delete booklet from its ID should return 200`() {
            bookletStateAdapter.init(
                listOf(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("100.00"),
                        label = "test",
                        owner = user,
                    )
                )
            )
            val bookletID = bookletStateAdapter.get().first().id!!

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                delete("/api/booklet/$bookletID")
            } Then {
                statusCode(200)
            }
            assertTrue(bookletStateAdapter.get().isEmpty())
        }

        @Test
        fun `Delete booklet from an ID of a booklet that does not exists should return 404`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                delete("/api/booklet/231")
            } Then {
                statusCode(404)
            }
        }
        @Test
        fun `Delete booklet from an ID of a booklet by a user that does not exists should return 404`() {
            Given {
                port(port)
                cookie(generateCookie(token))
                header("Content-Type", "application/json")
            } When {
                delete("/api/booklet/100")
            } Then {
                statusCode(404)
            }
        }
    }

    @Nested
    inner class FindByIdBookletEndpointTest {
        @Test
        fun `Request a Booklet from an existing ID must return 200 with the asking booklet`() {
            bookletStateAdapter.init(
                listOf(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("100.00"),
                        label = "test",
                        owner = user,
                    )
                )
            )
            val booklet = bookletStateAdapter.get().first()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/booklet/${booklet.id}")
            } Then {
                statusCode(200)
                body(
                    "amount", equalTo("100.00"),
                    "label", equalTo("test"),
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
                get("/api/booklet/0")
            } Then {
                statusCode(404)
            }
        }
    }
    @Nested
    inner class RequestBookletEndpointTest {

        @Test
        fun `Request for all booklets of a user must return 200 with the asking booklets`() {
            bookletStateAdapter.init(
                listOf(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("50.00"),
                        label = "test2",
                        owner = user,
                    ),
                    Booklet(
                        id = null,
                        amount = Amount.fromString("60.00"),
                        label = "test3",
                        owner = user,
                    ),
                    Booklet(
                        id = null,
                        amount = Amount.fromString("0.00"),
                        label = "test",
                        owner = user,
                    )
                )
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/booklet")
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
            bookletStateAdapter.init(
                listOf(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("1000.00"),
                        label = "Compte Épargne",
                        owner = user,
                    )
                )
            )
            val booklet = bookletStateAdapter.get().first()
            val currentDate = LocalDate.now()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", currentDate.monthValue)
                queryParam("year", currentDate.year)
            } When {
                get("/api/booklet/report/${booklet.id}")
            } Then {
                statusCode(200)
                body("label", equalTo("Compte Épargne"))
                body("realSold", equalTo("1000.00"))
            }
        }

        @Test
        fun `Request report for a booklet with explicit date range should return 200`() {
            bookletStateAdapter.init(
                listOf(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("1000.00"),
                        label = "Compte Épargne",
                        owner = user,
                    )
                )
            )
            val booklet = bookletStateAdapter.get().first()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", 4)
                queryParam("year", 2026)
                queryParam("startDate", "2026-03-28")
                queryParam("endDate", "2026-04-27")
            } When {
                get("/api/booklet/report/${booklet.id}")
            } Then {
                statusCode(200)
                body("label", equalTo("Compte Épargne"))
                body("realSold", equalTo("1000.00"))
            }
        }

        @Test
        fun `Request report with invalid date range should return 400`() {
            bookletStateAdapter.init(
                listOf(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("1000.00"),
                        label = "Compte Épargne",
                        owner = user,
                    )
                )
            )
            val booklet = bookletStateAdapter.get().first()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", 4)
                queryParam("year", 2026)
                queryParam("startDate", "2026-04-28")
                queryParam("endDate", "2026-04-27")
            } When {
                get("/api/booklet/report/${booklet.id}")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `Request report for a booklet with transactions should return all transactions`() {
            val booklet = Booklet(
                id = null,
                amount = Amount.fromString("1000.00"),
                label = "Compte Test",
                owner = user,
            )
            val currentDate = LocalDate.now()
            bookletStateAdapter.init(listOf(booklet))
            transactionStateTestAdapter.init(
                listOf(BookletTransaction(
                    user!!.id,
                    booklet.label,
                    transactions = listOf(
                        Transaction(
                            id = null,
                            label = "Transaction 2",
                            amount = Amount.fromString("50.00"),
                            date = currentDate,
                            isPreview = false,
                            isIncome = false,
                        ),
                        Transaction(
                            id = null,
                            label = "Transaction 1",
                            amount = Amount.fromString("100.00"),
                            date = currentDate,
                            isPreview = false,
                            isIncome = true,
                        )
                    ),
                    token = token
                ))
            )

            val savedBooklet = bookletStateAdapter.get().first()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", currentDate.monthValue)
                queryParam("year", currentDate.year)
            } When {
                get("/api/booklet/report/${savedBooklet.id}")
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
                get("/api/booklet/report/9999")
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Request report with invalid month should return 400`() {
            val element = Booklet(
                id = null,
                amount = Amount.fromString("1000.00"),
                label = "Test Booklet",
                owner = user,
            )
            bookletStateAdapter.init(
                listOf(
                    element
                )
            )
            val booklet = bookletStateAdapter.get().first()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", 13)
                queryParam("year", 2025)
            } When {
                get("/api/booklet/report/${booklet.id}")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `Request report with invalid year should handle correctly`() {
            bookletStateAdapter.init(
                listOf(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("500.00"),
                        label = "Test Booklet",
                        owner = user,
                    )
                )
            )
            val booklet = bookletStateAdapter.get().first()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", 1)
                queryParam("year", 2025)
            } When {
                get("/api/booklet/report/${booklet.id}")
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
                get("/api/booklet/report/1")
            } Then {
                statusCode(org.hamcrest.Matchers.either(equalTo(401)).or(equalTo(403)))
            }
        }

        @Test
        fun `Request report should return preview and real sold values`() {
            bookletStateAdapter.init(
                listOf(
                    Booklet(
                        id = null,
                        amount = Amount.fromString("2000.00"),
                        label = "Compte Principal",
                        owner = user,
                    )
                )
            )
            val booklet = bookletStateAdapter.get().first()
            val currentDate = LocalDate.now()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", currentDate.monthValue)
                queryParam("year", currentDate.year)
            } When {
                get("/api/booklet/report/${booklet.id}")
            } Then {
                statusCode(200)
                body("realSold", equalTo("2000.00"))
                body("previewSold", org.hamcrest.Matchers.notNullValue())
            }
        }
    }

    @Nested
    inner class BalancesAndTransactionsEndpointsTest {
        @Test
        fun `first generation with regular transactions should not fail between balances and transactions endpoints`() {
            val booklet = Booklet(
                id = null,
                amount = Amount.fromString("1000.00"),
                label = "Generation Test",
                owner = user,
            )
            bookletStateAdapter.init(listOf(booklet))
            val savedBooklet = bookletStateAdapter.get().first()
            val currentDate = LocalDate.now()

            regularTransactionStateAdapter.init(
                listOf(
                    BookletRegularTransactionInput(
                        userId = user!!.id,
                        bookletID = savedBooklet.id!!.toString(),
                        regularTransaction = RegularTransaction(
                            id = RegularTransactionId(java.util.UUID.randomUUID().toString()),
                            label = "Salaire",
                            amount = Amount.fromString("500.00"),
                            isIncome = true,
                            startDate = currentDate.withDayOfMonth(1),
                            frequencyProperty = FrequencyProperty.Forever(),
                            recurrenceRule = RecurrenceRule.Monthly(1)
                        )
                    )
                )
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", currentDate.monthValue)
                queryParam("year", currentDate.year)
            } When {
                get("/api/booklet/${savedBooklet.id}/balances")
            } Then {
                statusCode(200)
            }

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", currentDate.monthValue)
                queryParam("year", currentDate.year)
            } When {
                get("/api/booklet/${savedBooklet.id}/transactions")
            } Then {
                statusCode(200)
            }
        }
    }

    @Nested
    inner class RegenerateDeletedPrevisionalTransactionsTest {

        @Autowired
        private lateinit var regularTrackerStateRepository: RegularTrackerStateRepository

        @AfterEach
        fun clearTrackers() {
            regularTrackerStateRepository.clear()
        }

        @Test
        fun `GET transactions should return hasRegenerableTransactions false when no month is excluded`() {
            val booklet = Booklet(id = null, amount = Amount.fromString("1000.00"), label = "Test Regen Flag", owner = user)
            bookletStateAdapter.init(listOf(booklet))
            val savedBooklet = bookletStateAdapter.get().first()
            val currentDate = LocalDate.now()

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", currentDate.monthValue)
                queryParam("year", currentDate.year)
            } When {
                get("/api/booklet/${savedBooklet.id}/transactions")
            } Then {
                statusCode(200)
                body("hasRegenerableTransactions", equalTo(false))
            }
        }

        @Test
        fun `GET transactions should return hasRegenerableTransactions true when a month is excluded for a regular transaction`() {
            val booklet = Booklet(id = null, amount = Amount.fromString("1000.00"), label = "Test Regen Flag True", owner = user)
            bookletStateAdapter.init(listOf(booklet))
            val savedBooklet = bookletStateAdapter.get().first()

            val regularTxId = RegularTransactionId(java.util.UUID.randomUUID().toString())
            regularTransactionStateAdapter.init(
                listOf(
                    BookletRegularTransactionInput(
                        userId = user!!.id,
                        bookletID = savedBooklet.id!!.toString(),
                        regularTransaction = RegularTransaction(
                            id = regularTxId,
                            label = "Loyer",
                            amount = Amount.fromString("800.00"),
                            isIncome = false,
                            startDate = LocalDate.of(2024, 1, 1),
                            frequencyProperty = FrequencyProperty.Forever(),
                            recurrenceRule = RecurrenceRule.Monthly(5)
                        )
                    )
                )
            )

            regularTrackerStateRepository.init(
                listOf(
                    RegularTransactionTracker(
                        regularTransactionId = regularTxId,
                        bookletId = savedBooklet.id!!,
                        lastGeneratedDate = LocalDate.of(2024, 1, 5),
                        numberOfGeneratedTransaction = 1,
                        excludedMonths = setOf(YearMonth.of(2024, 1))
                    )
                )
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", 1)
                queryParam("year", 2024)
            } When {
                get("/api/booklet/${savedBooklet.id}/transactions")
            } Then {
                statusCode(200)
                body("hasRegenerableTransactions", equalTo(true))
            }
        }

        @Test
        fun `POST regenerate on non-existing booklet should return 404`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", 1)
                queryParam("year", 2024)
            } When {
                post("/api/booklet/00000000-0000-0000-0000-000000000000/transactions/regenerate")
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `POST regenerate on existing booklet with excluded month should return 200`() {
            val currentDate = LocalDate.now()
            val booklet = Booklet(id = null, amount = Amount.fromString("1000.00"), label = "Regen Test", owner = user)
            bookletStateAdapter.init(listOf(booklet))
            val savedBooklet = bookletStateAdapter.get().first()

            regularTransactionStateAdapter.init(
                listOf(
                    BookletRegularTransactionInput(
                        userId = user!!.id,
                        bookletID = savedBooklet.id!!.toString(),
                        regularTransaction = RegularTransaction(
                            id = RegularTransactionId(java.util.UUID.randomUUID().toString()),
                            label = "Loyer",
                            amount = Amount.fromString("800.00"),
                            isIncome = false,
                            startDate = currentDate.withDayOfMonth(1),
                            frequencyProperty = FrequencyProperty.Forever(),
                            recurrenceRule = RecurrenceRule.Monthly(1)
                        )
                    )
                )
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("month", currentDate.monthValue)
                queryParam("year", currentDate.year)
            } When {
                post("/api/booklet/${savedBooklet.id}/transactions/regenerate")
            } Then {
                statusCode(200)
            }
        }
    }
}
