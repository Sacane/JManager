package fr.sacane.jmanager.infrastructure.api


import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.infrastructure.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.BookletTransaction
import fr.sacane.jmanager.infrastructure.api.setup.TagStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.TransactionStateTestAdapter
import fr.sacane.jmanager.infrastructure.generateCookie
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class StatsControllerTest(
    @LocalServerPort val port: Int,
    @Autowired private val bookletStateTestAdapter: BookletStateTestAdapter,
    @Autowired private val transactionStateTestAdapter: TransactionStateTestAdapter,
    @Autowired private val tagStateTestAdapter: TagStateTestAdapter,
    @Autowired var objectMapper: ObjectMapper,
    @Autowired val tokenGenerator: TokenGenerator,
    @Autowired val tagRepository: TagRepository
) : AuthenticatedUserTest() {

    private lateinit var booklet: Booklet
    private lateinit var defaultTag: Tag

    @BeforeEach
    fun setup() {
        configureObjectMapper(objectMapper)
        defaultTag = tagRepository.defaultTag()

        bookletStateTestAdapter.init(
            listOf(Booklet(1000.toAmount(), "Compte Principal", owner = user))
        )
        booklet = bookletStateTestAdapter.get().first()
    }

    @AfterEach
    fun clear() {
        transactionStateTestAdapter.clear()
        bookletStateTestAdapter.clear()
        tagStateTestAdapter.clear()
    }

    @Nested
    inner class GetMonthlyBookletStatsEndpointTest {

        @Test
        fun `Get monthly booklet stats must send 200 and return stats`() {
            val year = LocalDate.now().year

            transactionStateTestAdapter.init(
                createTransaction(
                        listOf(
                            TransactionTestInput(
                            label = "Salaire",
                            amount = 2000.toAmount(),
                            isIncome = true,
                            date = LocalDate.of(year, 1, 15),
                        ),
                            TransactionTestInput(
                            label = "Courses",
                            isIncome = false,
                            amount = 150.toAmount(),
                            date = LocalDate.of(year, 1, 20),
                        )
                    )
                )
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/stats/monthly/{bookletId}/{year}", mapOf("bookletId" to booklet.id!!, "year" to year))
            } Then {
                statusCode(200)
                body(
                    "bookletId", equalTo(booklet.id!!.toString()),
                    "bookletLabel", equalTo("Compte Principal"),
                    "year", equalTo(year),
                    "monthlyData", notNullValue()
                )
            }
        }

        @Test
        fun `Get monthly booklet stats with unknown booklet must send 404`() {
            val year = LocalDate.now().year

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/stats/monthly/{bookletId}/{year}", mapOf("bookletId" to UUID.randomUUID(), "year" to year))
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Get monthly booklet stats with unauthenticated user must send 401`() {
            val year = LocalDate.now().year

            Given {
                port(port)
                cookie(generateCookie(tokenGenerator.generateToken(UserId(UUID.randomUUID()), "test", setOf(Role.USER)).tokenValue))
                header("Content-Type", "application/json")
            } When {
                get("/api/stats/monthly/{bookletId}/{year}", mapOf("bookletId" to booklet.id!!, "year" to year))
            } Then {
                statusCode(401)
            }
        }
    }

    @Nested
    inner class GetCategoryDistributionEndpointTest {

        @Test
        fun `Get category distribution must send 200 and return distribution`() {

            transactionStateTestAdapter.init(
                createTransaction(
                    listOf(
                        TransactionTestInput(
                            label = "Courses",
                            amount = (150).toAmount(),
                            isIncome = false,
                            date = LocalDate.now(),
                        ),
                        TransactionTestInput(
                            label = "Restaurant",
                            amount = 50.toAmount(),
                            isIncome = false,
                            date = LocalDate.now()
                        )
                    )
                )
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/stats/category-distribution")
            } Then {
                statusCode(200)
                body(
                    "categories", notNullValue(),
                    "categories[0].colorDTO", notNullValue(),
                    "totalExpenses", notNullValue()
                )
            }
        }

        @Test
        fun `Get category distribution with no transactions must send 200 with empty data`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/stats/category-distribution")
            } Then {
                statusCode(200)
                body(
                    "totalExpenses", equalTo("0.00")
                )
            }
        }

        @Test
        fun `Get category distribution with unauthenticated user must send 401`() {
            Given {
                port(port)
                cookie(generateCookie(tokenGenerator.generateToken(UserId(UUID.randomUUID()), "test", setOf(Role.USER)).tokenValue))
                header("Content-Type", "application/json")
            } When {
                get("/api/stats/category-distribution")
            } Then {
                statusCode(401)
            }
        }
    }

    @Nested
    inner class GetTrendStatsEndpointTest {

        @Test
        fun `Get trend stats must send 200 and return trends`() {
            val currentDate = LocalDate.now()

            val initialState = listOf(
                TransactionTestInput(
                    label = "Transaction 1",
                    amount = 1000.toAmount(),
                    isIncome = true,
                    date = currentDate.minusMonths(2),
                ),
                TransactionTestInput(
                    label = "Transaction 2",
                    amount = (500).toAmount(),
                    date = currentDate.minusMonths(1),
                    isIncome = false,
                ),
                TransactionTestInput(
                    label = "Transaction 3",
                    amount = 2000.toAmount(),
                    date = currentDate,
                    isIncome = false
                )
            )
            transactionStateTestAdapter.init(
                createTransaction(initialState),
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/stats/trends")
            } Then {
                statusCode(200)
                body(
                    "monthlyTrends", notNullValue()
                )
            }
        }

        @Test
        fun `Get trend stats with no transactions must send 200 with empty trends`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/stats/trends")
            } Then {
                statusCode(200)
                body(
                    "monthlyTrends", notNullValue()
                )
            }
        }

        @Test
        fun `Get trend stats with unauthenticated user must send 401`() {
            Given {
                port(port)
                cookie(generateCookie(tokenGenerator.generateToken(UserId(UUID.randomUUID()), "test", setOf(Role.USER)).tokenValue))
                header("Content-Type", "application/json")
            } When {
                get("/api/stats/trends")
            } Then {
                statusCode(401)
            }
        }
    }

    @Nested
    inner class GetPrevisionalTransactionsEndpointTest {

        @Test
        fun `Get previsional transactions must send 200 and return transactions`() {
            val startDate = LocalDate.now()
            val endDate = startDate.plusMonths(3)

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("startDate", startDate.toString())
                queryParam("endDate", endDate.toString())
            } When {
                get("/api/stats/previsional")
            } Then {
                statusCode(200)
                body(
                    "transactions", notNullValue(),
                    "groupedByBooklet", notNullValue(),
                    "totalAmount", notNullValue(),
                    "totalIncome", notNullValue(),
                    "totalExpenses", notNullValue(),
                    "startDate", equalTo(startDate.toString()),
                    "endDate", equalTo(endDate.toString())
                )
            }
        }

        @Test
        fun `Get previsional transactions with invalid date range must send 400`() {
            val startDate = LocalDate.now()
            val endDate = startDate.minusDays(1)

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("startDate", startDate.toString())
                queryParam("endDate", endDate.toString())
            } When {
                get("/api/stats/previsional")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `Get previsional transactions with unauthenticated user must send 401`() {
            val startDate = LocalDate.now()
            val endDate = startDate.plusMonths(3)

            Given {
                port(port)
                cookie(generateCookie(tokenGenerator.generateToken(UserId(UUID.randomUUID()), "test", setOf(Role.USER)).tokenValue))
                header("Content-Type", "application/json")
                queryParam("startDate", startDate.toString())
                queryParam("endDate", endDate.toString())
            } When {
                get("/api/stats/previsional")
            } Then {
                statusCode(401)
            }
        }

        @Test
        fun `Get previsional transactions without dates must send 400`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/stats/previsional")
            } Then {
                statusCode(400)
            }
        }
    }
    private fun createTransaction(
        inputs: List<TransactionTestInput> = listOf()
    ): List<BookletTransaction> {
        return listOf(BookletTransaction(
            transactions = inputs.mapIndexed { index, it ->
                Transaction(
                    id = null,
                    label = it.label,
                    amount = it.amount,
                    isIncome = it.isIncome,
                    date = it.date,
                    tag = defaultTag
                )
            },
            bookletOwnerId = user!!.id,
            bookletName = booklet.label,
            token = token.asTokenUUID()
        ))
    }
}

data class TransactionTestInput (
    val label: String,
    val amount: Amount,
    val isIncome: Boolean,
    val date: LocalDate
)
