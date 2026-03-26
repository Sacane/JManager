package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.AccountTransaction
import fr.sacane.jmanager.infrastructure.api.setup.BookletRegularTransactionInput
import fr.sacane.jmanager.infrastructure.api.setup.RegularTransactionStateForTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.RegularTrackerStateRepository
import fr.sacane.jmanager.infrastructure.api.setup.TransactionStateTestAdapter
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    @Autowired private val regularTransactionStateForTestAdapter: RegularTransactionStateForTestAdapter,
    @Autowired private val accountStateTestAdapter: AccountStateTestAdapter,
    @Autowired private val transactionStateTestAdapter: TransactionStateTestAdapter,
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
        transactionStateTestAdapter.clear()
        regularTransactionStateForTestAdapter.clear()
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

            val createdTransactions = regularTransactionStateForTestAdapter.get()
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

            val regularTransaction = RegularTransaction(
                id = RegularTransactionId(""),
                label = "Salaire",
                amount = 2000.00.toAmount(),
                isIncome = true,
                tag = tagDTO.toDomain(),
                frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(1),
                startDate = LocalDate.now(),
                recurrenceRule = RecurrenceRule.Monthly(15)
            )

            regularTransactionStateForTestAdapter.init(
                listOf(
                    BookletRegularTransactionInput(
                        userId = user!!.id,
                        bookletID = booklet.id!!.toString(),
                        regularTransaction = regularTransaction
                    )
                )
            )

            val createdTransaction = regularTransactionStateForTestAdapter.get().first()
            val transactionId = createdTransaction.id.value

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

            val regularTransactions = listOf(
                RegularTransaction(
                    id = RegularTransactionId(""),
                    label = "Salaire",
                    amount = 2000.00.toAmount(),
                    isIncome = true,
                    tag = tagDTO.toDomain(),
                    frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(1),
                    startDate = LocalDate.now(),
                    recurrenceRule = RecurrenceRule.Monthly(15)
                ),
                RegularTransaction(
                    id = RegularTransactionId(""),
                    label = "Loyer",
                    amount = 800.00.toAmount(),
                    isIncome = false,
                    tag = tagDTO.toDomain(),
                    frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(1),
                    startDate = LocalDate.now(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )
            )

            regularTransactionStateForTestAdapter.init(
                regularTransactions.map {
                    BookletRegularTransactionInput(
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

            val transactions = regularTransactionStateForTestAdapter.get()
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

    @Nested
    inner class UpdateRegularTransactionEndpointTest {

        @Test
        fun `Update regular transaction must send 200 and return updated transaction`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val booklet = accountStateTestAdapter.get().find { it.label == "test" }!!

            val regularTransaction = RegularTransaction(
                id = RegularTransactionId(""),
                label = "Initial Label",
                amount = 100.00.toAmount(),
                isIncome = false,
                tag = tagDTO.toDomain(),
                frequencyProperty = FrequencyProperty.Forever(),
                startDate = LocalDate.now(),
                recurrenceRule = RecurrenceRule.Monthly(15)
            )

            regularTransactionStateForTestAdapter.init(
                listOf(
                    BookletRegularTransactionInput(
                        userId = user!!.id,
                        bookletID = booklet.id!!.toString(),
                        regularTransaction = regularTransaction
                    )
                )
            )

            val createdTransaction = regularTransactionStateForTestAdapter.get().first()
            val transactionId = createdTransaction.id.value

            val updateRequest = mapOf(
                "id" to transactionId,
                "label" to "Updated Label",
                "value" to 200.00,
                "isIncome" to true,
                "tagDTO" to mapOf(
                    "tagId" to tagDTO.tagId,
                    "label" to tagDTO.label,
                    "colorDTO" to mapOf(
                        "red" to tagDTO.colorDTO.red,
                        "green" to tagDTO.colorDTO.green,
                        "blue" to tagDTO.colorDTO.blue
                    )
                ),
                "frequencyProperty" to mapOf(
                    "type" to "FOREVER"
                ),
                "bookletIds" to listOf(booklet.id!!.toString()),
                "recurrenceRule" to mapOf(
                    "type" to "MONTHLY",
                    "value" to 20
                )
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(updateRequest))
            } When {
                patch("/api/transaction/regular")
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("Updated Label"),
                    "value", equalTo("200.00"),
                    "isIncome", equalTo(true)
                )
            }
        }

        @Test
        fun `Update non-existing regular transaction must send 404`() {
            val updateRequest = mapOf(
                "id" to UUID.randomUUID().toString(),
                "label" to "Updated Label",
                "value" to 200.00,
                "isIncome" to true,
                "tagDTO" to mapOf(
                    "tagId" to tagDTO.tagId,
                    "label" to tagDTO.label,
                    "colorDTO" to mapOf(
                        "red" to tagDTO.colorDTO.red,
                        "green" to tagDTO.colorDTO.green,
                        "blue" to tagDTO.colorDTO.blue
                    )
                ),
                "frequencyProperty" to mapOf(
                    "type" to "FOREVER"
                ),
                "bookletIds" to listOf(UUID.randomUUID().toString()),
                "recurrenceRule" to mapOf(
                    "type" to "MONTHLY",
                    "value" to 20
                )
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(updateRequest))
            } When {
                patch("/api/transaction/regular")
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Update regular transaction with unauthenticated user must send 404`() {
            val updateRequest = mapOf(
                "id" to UUID.randomUUID().toString(),
                "label" to "Updated Label",
                "value" to 200.00,
                "isIncome" to true,
                "tagDTO" to mapOf(
                    "tagId" to tagDTO.tagId,
                    "label" to tagDTO.label,
                    "colorDTO" to mapOf(
                        "red" to tagDTO.colorDTO.red,
                        "green" to tagDTO.colorDTO.green,
                        "blue" to tagDTO.colorDTO.blue
                    )
                ),
                "frequencyProperty" to mapOf(
                    "type" to "FOREVER"
                ),
                "bookletIds" to listOf(UUID.randomUUID().toString()),
                "recurrenceRule" to mapOf(
                    "type" to "MONTHLY",
                    "value" to 20
                )
            )

            Given {
                port(port)
                cookie(generateCookie(tokenGenerator.generateToken(UserId(UUID.randomUUID()), "test", setOf(Role.USER)).tokenValue))
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(updateRequest))
            } When {
                patch("/api/transaction/regular")
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Update regular transaction changing only label must send 200`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val booklet = accountStateTestAdapter.get().find { it.label == "test" }!!

            val regularTransaction = RegularTransaction(
                id = RegularTransactionId(""),
                label = "Original",
                amount = 100.00.toAmount(),
                isIncome = false,
                tag = tagDTO.toDomain(),
                frequencyProperty = FrequencyProperty.Forever(),
                startDate = LocalDate.now(),
                recurrenceRule = RecurrenceRule.Monthly(15)
            )

            regularTransactionStateForTestAdapter.init(
                listOf(
                    BookletRegularTransactionInput(
                        userId = user!!.id,
                        bookletID = booklet.id!!.toString(),
                        regularTransaction = regularTransaction
                    )
                )
            )

            val createdTransaction = regularTransactionStateForTestAdapter.get().first()

            val updateRequest = mapOf(
                "id" to createdTransaction.id.value,
                "label" to "Modified Label",
                "value" to 100.00,
                "isIncome" to false,
                "tagDTO" to mapOf(
                    "tagId" to tagDTO.tagId,
                    "label" to tagDTO.label,
                    "colorDTO" to mapOf(
                        "red" to tagDTO.colorDTO.red,
                        "green" to tagDTO.colorDTO.green,
                        "blue" to tagDTO.colorDTO.blue
                    )
                ),
                "frequencyProperty" to mapOf(
                    "type" to "FOREVER"
                ),
                "bookletIds" to listOf(booklet.id!!.toString()),
                "recurrenceRule" to mapOf(
                    "type" to "MONTHLY",
                    "value" to 15
                )
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(updateRequest))
            } When {
                patch("/api/transaction/regular")
            } Then {
                statusCode(200)
                body("label", equalTo("Modified Label"))
            }
        }

        @Test
        fun `Update regular transaction must update associated booklets`() {
            accountStateTestAdapter.init(
                listOf(
                    Booklet(200.toAmount(), "test-a", owner = user),
                    Booklet(300.toAmount(), "test-b", owner = user)
                )
            )
            val booklets = accountStateTestAdapter.get().toList()
            val bookletA = booklets.first { it.label == "test-a" }
            val bookletB = booklets.first { it.label == "test-b" }

            val regularTransaction = RegularTransaction(
                id = RegularTransactionId(""),
                label = "Switch booklet",
                amount = 100.00.toAmount(),
                isIncome = false,
                tag = tagDTO.toDomain(),
                frequencyProperty = FrequencyProperty.Forever(),
                startDate = LocalDate.now(),
                recurrenceRule = RecurrenceRule.Monthly(15)
            )

            regularTransactionStateForTestAdapter.init(
                listOf(
                    BookletRegularTransactionInput(
                        userId = user!!.id,
                        bookletID = bookletA.id!!.toString(),
                        regularTransaction = regularTransaction
                    )
                )
            )

            val createdTransaction = regularTransactionStateForTestAdapter.get().first()

            val updateRequest = mapOf(
                "id" to createdTransaction.id.value,
                "label" to "Switch booklet updated",
                "value" to 100.00,
                "isIncome" to false,
                "tagDTO" to mapOf(
                    "tagId" to tagDTO.tagId,
                    "label" to tagDTO.label,
                    "colorDTO" to mapOf(
                        "red" to tagDTO.colorDTO.red,
                        "green" to tagDTO.colorDTO.green,
                        "blue" to tagDTO.colorDTO.blue
                    )
                ),
                "frequencyProperty" to mapOf("type" to "FOREVER"),
                "bookletIds" to listOf(bookletB.id!!.toString()),
                "recurrenceRule" to mapOf(
                    "type" to "MONTHLY",
                    "value" to 15
                )
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(updateRequest))
            } When {
                patch("/api/transaction/regular")
            } Then {
                statusCode(200)
                body("bookletIds", hasItems(bookletB.id!!.toString()))
            }

            val refreshed = regularTransactionStateForTestAdapter.get().first { it.id.value == createdTransaction.id.value }
            assertTrue(refreshed.associatedBooklets.any { it.id == bookletB.id })
            assertTrue(refreshed.associatedBooklets.none { it.id == bookletA.id })
        }
    }

    @Nested
    inner class DeleteRegularTransactionEndpointTest {

        @Test
        fun `Delete regular transaction must send 200`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val booklet = accountStateTestAdapter.get().find { it.label == "test" }!!

            val regularTransaction = RegularTransaction(
                id = RegularTransactionId(""),
                label = "To Delete",
                amount = 100.00.toAmount(),
                isIncome = false,
                tag = tagDTO.toDomain(),
                frequencyProperty = FrequencyProperty.Forever(),
                startDate = LocalDate.now(),
                recurrenceRule = RecurrenceRule.Monthly(15)
            )

            regularTransactionStateForTestAdapter.init(
                listOf(
                    BookletRegularTransactionInput(
                        userId = user!!.id,
                        bookletID = booklet.id!!.toString(),
                        regularTransaction = regularTransaction
                    )
                )
            )

            val createdTransaction = regularTransactionStateForTestAdapter.get().first()
            val transactionId = createdTransaction.id.value

            Given {
                port(port)
                cookie("token", token)
            } When {
                delete("/api/transaction/regular/{id}", mapOf("id" to transactionId))
            } Then {
                statusCode(200)
            }

            val remainingTransactions = regularTransactionStateForTestAdapter.get()
            assertEquals(0, remainingTransactions.size)
        }

        @Test
        fun `Delete non-existing regular transaction must send 404`() {
            Given {
                port(port)
                cookie("token", token)
            } When {
                delete("/api/transaction/regular/{id}", mapOf("id" to UUID.randomUUID().toString()))
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Delete regular transaction with unauthenticated user must send 404`() {
            Given {
                port(port)
                cookie(generateCookie(tokenGenerator.generateToken(UserId(UUID.randomUUID()), "test", setOf(Role.USER)).tokenValue))
            } When {
                delete("/api/transaction/regular/{id}", mapOf("id" to UUID.randomUUID().toString()))
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Delete regular transaction must not affect other transactions`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val booklet = accountStateTestAdapter.get().find { it.label == "test" }!!

            val regularTransactions = listOf(
                RegularTransaction(
                    id = RegularTransactionId(""),
                    label = "Transaction 1",
                    amount = 100.00.toAmount(),
                    isIncome = false,
                    tag = tagDTO.toDomain(),
                    frequencyProperty = FrequencyProperty.Forever(),
                    startDate = LocalDate.now(),
                    recurrenceRule = RecurrenceRule.Monthly(15)
                ),
                RegularTransaction(
                    id = RegularTransactionId(""),
                    label = "Transaction 2",
                    amount = 200.00.toAmount(),
                    isIncome = true,
                    tag = tagDTO.toDomain(),
                    frequencyProperty = FrequencyProperty.Forever(),
                    startDate = LocalDate.now(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )
            )

            regularTransactionStateForTestAdapter.init(
                regularTransactions.map {
                    BookletRegularTransactionInput(
                        userId = user!!.id,
                        bookletID = booklet.id!!.toString(),
                        regularTransaction = it
                    )
                }
            )

            val allTransactions = regularTransactionStateForTestAdapter.get()
            assertEquals(2, allTransactions.size)

            val transactionToDelete = allTransactions.first()
            val firstTransactionId = transactionToDelete.id.value

            Given {
                port(port)
                cookie("token", token)
            } When {
                delete("/api/transaction/regular/{id}", mapOf("id" to firstTransactionId))
            } Then {
                statusCode(200)
            }

            val remainingTransactions = regularTransactionStateForTestAdapter.get()
            assertEquals(1, remainingTransactions.size)
            assertTrue(remainingTransactions.first().id.value != transactionToDelete.id.value)
        }

        @Test
        fun `Delete regular transaction linked to multiple booklets must cleanup links and trackers while keeping generated sheets`() {
            accountStateTestAdapter.init(
                listOf(
                    Booklet(200.toAmount(), "booklet-a", owner = user),
                    Booklet(300.toAmount(), "booklet-b", owner = user)
                )
            )
            val booklets = accountStateTestAdapter.get().toList()
            val bookletA = booklets.first { it.label == "booklet-a" }
            val bookletB = booklets.first { it.label == "booklet-b" }

            val request = MonthlyRegularTransactionRequest(
                label = "Subscription Multi",
                value = BigDecimal(49.99),
                startDate = LocalDate.now(),
                isIncome = false,
                tagDTO = tagDTO,
                frequencyProperty = FrequencyPropertyDTO(
                    type = FrequencyPropertyType.FOREVER,
                    untilDate = null,
                    times = null
                ),
                repeatDay = 10,
                bookletIds = listOf(bookletA.id!!.toString(), bookletB.id!!.toString())
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
            }

            val createdRegular = regularTransactionStateForTestAdapter.get().first { it.label == "Subscription Multi" }

            regularTrackerStateRepository.init(
                listOf(
                    RegularTransactionTracker(
                        regularTransactionId = createdRegular.id,
                        bookletId = bookletA.id!!,
                        lastGeneratedDate = LocalDate.now().minusMonths(1),
                        numberOfGeneratedTransaction = 2
                    ),
                    RegularTransactionTracker(
                        regularTransactionId = createdRegular.id,
                        bookletId = bookletB.id!!,
                        lastGeneratedDate = LocalDate.now().minusMonths(1),
                        numberOfGeneratedTransaction = 1
                    )
                )
            )

            transactionStateTestAdapter.init(
                listOf(
                    AccountTransaction(
                        accountOwnerId = user!!.id,
                        accountName = bookletA.label,
                        token = token,
                        transactions = listOf(
                            Transaction(
                                id = null,
                                label = "Generated A",
                                date = LocalDate.now(),
                                amount = 49.99.toAmount(),
                                isIncome = false,
                                tag = tagRepository.defaultTag(),
                                regularTransactionId = createdRegular.id
                            )
                        )
                    ),
                    AccountTransaction(
                        accountOwnerId = user!!.id,
                        accountName = bookletB.label,
                        token = token,
                        transactions = listOf(
                            Transaction(
                                id = null,
                                label = "Generated B",
                                date = LocalDate.now(),
                                amount = 49.99.toAmount(),
                                isIncome = false,
                                tag = tagRepository.defaultTag(),
                                regularTransactionId = createdRegular.id
                            )
                        )
                    )
                )
            )

            val createdBeforeDelete = transactionStateTestAdapter.get()
                .filter { it.label == "Generated A" || it.label == "Generated B" }
            assertEquals(2, createdBeforeDelete.size)
            assertTrue(createdBeforeDelete.all { it.regularTransactionId?.value == createdRegular.id.value })

            Given {
                port(port)
                cookie("token", token)
            } When {
                delete("/api/transaction/regular/{id}", mapOf("id" to createdRegular.id.value))
            } Then {
                statusCode(200)
            }

            val remainingRegulars = regularTransactionStateForTestAdapter.get()
            assertTrue(remainingRegulars.none { it.id.value == createdRegular.id.value })

            val remainingTrackers = regularTrackerStateRepository.get()
            assertTrue(remainingTrackers.none { it.regularTransactionId.value == createdRegular.id.value })

            val generatedAfterDelete = transactionStateTestAdapter.get()
                .filter { it.label == "Generated A" || it.label == "Generated B" }
            assertEquals(2, generatedAfterDelete.size)
            assertNotNull(generatedAfterDelete.find { it.label == "Generated A" })
            assertNotNull(generatedAfterDelete.find { it.label == "Generated B" })
            assertTrue(generatedAfterDelete.all { it.regularTransactionId == null })
            assertNull(generatedAfterDelete.first { it.label == "Generated A" }.regularTransactionId)
            assertNull(generatedAfterDelete.first { it.label == "Generated B" }.regularTransactionId)
        }

        @Test
        fun `Bulk delete regular transactions must send 200 and delete all selected`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val booklet = accountStateTestAdapter.get().find { it.label == "test" }!!

            val regularTransactions = listOf(
                RegularTransaction(
                    id = RegularTransactionId(""),
                    label = "Bulk 1",
                    amount = 100.00.toAmount(),
                    isIncome = false,
                    tag = tagDTO.toDomain(),
                    frequencyProperty = FrequencyProperty.Forever(),
                    startDate = LocalDate.now(),
                    recurrenceRule = RecurrenceRule.Monthly(15)
                ),
                RegularTransaction(
                    id = RegularTransactionId(""),
                    label = "Bulk 2",
                    amount = 80.00.toAmount(),
                    isIncome = false,
                    tag = tagDTO.toDomain(),
                    frequencyProperty = FrequencyProperty.Forever(),
                    startDate = LocalDate.now(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )
            )

            regularTransactionStateForTestAdapter.init(
                regularTransactions.map {
                    BookletRegularTransactionInput(
                        userId = user!!.id,
                        bookletID = booklet.id!!.toString(),
                        regularTransaction = it
                    )
                }
            )

            val transactionIds = regularTransactionStateForTestAdapter.get().map { it.id.value }

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(mapOf("transactionIds" to transactionIds)))
            } When {
                delete("/api/transaction/regular")
            } Then {
                statusCode(200)
                body("deletedIds", hasItems(*transactionIds.toTypedArray()))
            }

            val remaining = regularTransactionStateForTestAdapter.get()
            assertTrue(remaining.isEmpty())
        }

        @Test
        fun `Bulk delete regular transactions with empty selection must send 400`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(mapOf("transactionIds" to emptyList<String>())))
            } When {
                delete("/api/transaction/regular")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `Bulk delete regular transactions with unknown id must send 404`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(mapOf("transactionIds" to listOf(UUID.randomUUID().toString()))))
            } When {
                delete("/api/transaction/regular")
            } Then {
                statusCode(404)
            }
        }
    }
}