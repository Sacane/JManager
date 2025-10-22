package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyRepeatProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.TagRepository
import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.BookletMonthlyTransactionInput
import fr.sacane.jmanager.infrastructure.api.setup.MonthlyTransactionStateForTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.RegularTrackerStateRepository
import fr.sacane.jmanager.infrastructure.api.transaction.FrequencyPropertyDTO
import fr.sacane.jmanager.infrastructure.api.transaction.FrequencyPropertyType
import fr.sacane.jmanager.infrastructure.api.transaction.MonthlyRegularTransactionRequest
import fr.sacane.jmanager.infrastructure.generateCookie
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItems
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class RegularTransactionControllerTest(
    @LocalServerPort val port: Int,
    @Autowired private val monthlyTransactionStateForTestAdapter: MonthlyTransactionStateForTestAdapter,
    @Autowired private val accountStateTestAdapter: AccountStateTestAdapter,
    @Autowired private val regularTrackerStateRepository: RegularTrackerStateRepository,
    @Autowired var objectMapper: ObjectMapper,
    @Autowired val tokenGenerator: TokenGenerator,
    @Autowired val tagRepository: TagRepository
): AuthenticatedUserTest() {


    val tagDTO = tagRepository.defaultTag()?.toDTO()!!


    @BeforeEach
    fun setup() {
        configureObjectMapper(objectMapper)
    }

    @AfterEach
    fun clear() {
        monthlyTransactionStateForTestAdapter.clear()
        accountStateTestAdapter.clear()
        regularTrackerStateRepository.clear()
    }

    @Nested
    inner class CreateMonthlyTransactionEndpointTest {

        @Test
        fun `Create a monthly transaction must send 200`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val booklet = accountStateTestAdapter.get().find { it.label == "test" }!!

            val request = MonthlyRegularTransactionRequest(
                label = "Salaire",
                value = BigDecimal(2000.00),
                startDate = LocalDate.now(),
                isIncome = true,
                tagDTO = tagDTO,
                frequencyProperty = FrequencyPropertyDTO(
                    type = FrequencyPropertyType.FOREVER,
                    untilDate = null,
                    times = null
                ),
                repeatDay = 15,
                bookletIds = listOf(booklet.id!!.toString())
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(request))
            } When {
                post("/api/transaction/monthly")
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("Salaire"),
                    "value", equalTo("2000.00"),
                    "isIncome", equalTo(true)
                )
            }

            val createdTransactions = monthlyTransactionStateForTestAdapter.get()
            assertEquals(1, createdTransactions.size)
            assertEquals("Salaire", createdTransactions.first().label)
        }

        @Test
        fun `Create a monthly transaction without repeatDay must send 200`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val booklet = accountStateTestAdapter.get().find { it.label == "test" }!!

            val request = MonthlyRegularTransactionRequest(
                label = "Loyer",
                value = BigDecimal(800.00),
                startDate = LocalDate.now(),
                isIncome = false,
                tagDTO = tagDTO,
                frequencyProperty = FrequencyPropertyDTO(
                    type = FrequencyPropertyType.TIMES,
                    untilDate = null,
                    times = 12
                ),
                repeatDay = null,
                bookletIds = listOf(booklet.id!!.toString())
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(request))
            } When {
                post("/api/transaction/monthly")
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("Loyer"),
                    "value", equalTo("800.00"),
                    "isIncome", equalTo(false)
                )
            }
        }

        @Test
        fun `Create a monthly transaction with multiple booklets must send 200`() {
            accountStateTestAdapter.init(
                listOf(
                    Booklet(200.toAmount(), "test1", owner = user),
                    Booklet(300.toAmount(), "test2", owner = user)
                )
            )
            val booklets = accountStateTestAdapter.get()
            val bookletIds = booklets.mapNotNull { it.id?.toString() }

            val request = MonthlyRegularTransactionRequest(
                label = "Abonnement",
                value = BigDecimal(50.00),
                startDate = LocalDate.now(),
                isIncome = false,
                tagDTO = tagDTO,
                frequencyProperty = FrequencyPropertyDTO(
                    type = FrequencyPropertyType.UNTIL_DATE,
                    untilDate = LocalDate.now().plusYears(1),
                    times = null
                ),
                repeatDay = 1,
                bookletIds = bookletIds
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(request))
            } When {
                post("/api/transaction/monthly")
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("Abonnement"),
                    "value", equalTo("50.00")
                )
            }
        }

        @Test
        fun `Create a monthly transaction with future start date must send 200`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val booklet = accountStateTestAdapter.get().find { it.label == "test" }!!

            val futureDate = LocalDate.now().plusMonths(2)
            val request = MonthlyRegularTransactionRequest(
                label = "Future Transaction",
                value = BigDecimal(150.00),
                startDate = futureDate,
                isIncome = true,
                tagDTO = tagDTO,
                frequencyProperty = FrequencyPropertyDTO(
                    type = FrequencyPropertyType.FOREVER,
                    untilDate = null,
                    times = null
                ),
                repeatDay = 5,
                bookletIds = listOf(booklet.id!!.toString())
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(request))
            } When {
                post("/api/transaction/monthly")
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("Future Transaction"),
                    "value", equalTo("150.00")
                )
            }
        }

        @Test
        fun `Create a monthly transaction with unknown booklet must send 404`() {
            val request = MonthlyRegularTransactionRequest(
                label = "Test",
                value = BigDecimal(100.00),
                startDate = LocalDate.now(),
                isIncome = true,
                tagDTO = tagDTO,
                frequencyProperty = FrequencyPropertyDTO(
                    type = FrequencyPropertyType.FOREVER,
                    untilDate = null,
                    times = null
                ),
                repeatDay = 15,
                bookletIds = listOf(UUID.randomUUID().toString())
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(request))
            } When {
                post("/api/transaction/monthly")
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Create a monthly transaction with unauthenticated user must send 404`() {
            val request = MonthlyRegularTransactionRequest(
                label = "Test",
                value = BigDecimal(100.00),
                startDate = LocalDate.now(),
                isIncome = true,
                tagDTO = tagDTO,
                frequencyProperty = FrequencyPropertyDTO(
                    type = FrequencyPropertyType.FOREVER,
                    untilDate = null,
                    times = null
                ),
                repeatDay = 15,
                bookletIds = listOf(UUID.randomUUID().toString())
            )

            Given {
                port(port)
                cookie(generateCookie(tokenGenerator.generateToken(UserId(UUID.randomUUID()), "test", setOf(Role.USER)).tokenValue))
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(request))
            } When {
                post("/api/transaction/monthly")
            } Then {
                statusCode(404)
            }
        }
    }

    @Nested
    inner class GetRegularTransactionByIdEndpointTest {

        @Test
        fun `Get regular transaction by id must send 200 and return the transaction`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val booklet = accountStateTestAdapter.get().find { it.label == "test" }!!

            val monthlyTransaction = MonthlyTransaction(
                id = RegularTransactionId(""),
                label = "Salaire",
                amount = 2000.00.toAmount(),
                isIncome = true,
                tag = tagDTO.toDomain(),
                frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(1),
                startDate = LocalDate.now(),
                monthlyRepeatProperty = MonthlyRepeatProperty(15)
            )

            monthlyTransactionStateForTestAdapter.init(
                listOf(
                    BookletMonthlyTransactionInput(
                        userId = user!!.id,
                        bookletID = booklet.id!!.toString(),
                        regularTransaction = monthlyTransaction
                    )
                )
            )

            val createdTransaction = monthlyTransactionStateForTestAdapter.get().first()
            val transactionId = (createdTransaction as MonthlyTransaction).id.value

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/transaction/regular/{id}", mapOf("id" to transactionId))
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("Salaire"),
                    "value", equalTo("2000.00"),
                    "isIncome", equalTo(true)
                )
            }
        }

        @Test
        fun `Get regular transaction with unknown id must send 404`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/transaction/regular/{id}", mapOf("id" to "00000000-0000-0000-0000-000000000000"))
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Get regular transaction with unauthenticated user must send 404`() {
            Given {
                port(port)
                cookie(generateCookie(tokenGenerator.generateToken(UserId(UUID.randomUUID()), "test", setOf(Role.USER)).tokenValue))
                header("Content-Type", "application/json")
            } When {
                get("/api/transaction/regular/{id}", mapOf("id" to "00000000-0000-0000-0000-000000000000"))
            } Then {
                statusCode(404)
            }
        }
    }

    @Nested
    inner class GetAllRegularTransactionsEndpointTest {

        @Test
        fun `Get all regular transactions must send 200 and return all transactions`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val booklet = accountStateTestAdapter.get().find { it.label == "test" }!!

            val monthlyTransactions = listOf(
                MonthlyTransaction(
                    id = RegularTransactionId(""),
                    label = "Salaire",
                    amount = 2000.00.toAmount(),
                    isIncome = true,
                    tag = tagDTO.toDomain(),
                    frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(1),
                    startDate = LocalDate.now(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(15)
                ),
                MonthlyTransaction(
                    id = RegularTransactionId(""),
                    label = "Loyer",
                    amount = 800.00.toAmount(),
                    isIncome = false,
                    tag = tagDTO.toDomain(),
                    frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(1),
                    startDate = LocalDate.now(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
                )
            )

            monthlyTransactionStateForTestAdapter.init(
                monthlyTransactions.map {
                    BookletMonthlyTransactionInput(
                        userId = user!!.id,
                        bookletID = booklet.id!!.toString(),
                        regularTransaction = it
                    )
                }
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/transaction/regular")
            } Then {
                statusCode(200)
                body(
                    "label", hasItems("Salaire", "Loyer"),
                    "size()", equalTo(2)
                )
            }

            val transactions = monthlyTransactionStateForTestAdapter.get()
            assertEquals(2, transactions.size)
        }

        @Test
        fun `Get all regular transactions with no transactions must send 200 with empty list`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/transaction/regular")
            } Then {
                statusCode(200)
                body("size()", equalTo(0))
            }
        }

        @Test
        fun `Get all regular transactions with unauthenticated user must send 404`() {
            Given {
                port(port)
                cookie(generateCookie(tokenGenerator.generateToken(UserId(UUID.randomUUID()), "test", setOf(Role.USER)).tokenValue))
                header("Content-Type", "application/json")
            } When {
                get("/api/transaction/regular")
            } Then {
                statusCode(404)
            }
        }
    }
}