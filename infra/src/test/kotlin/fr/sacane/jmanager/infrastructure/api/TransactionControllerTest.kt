package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.AccountTransaction
import fr.sacane.jmanager.infrastructure.api.setup.TransactionStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.transaction.*
import fr.sacane.jmanager.infrastructure.generateCookie
import fr.sacane.jmanager.infrastructure.spi.repositories.TransactionJpaRepository
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.util.UUID

class LocalDateSerializer : JsonSerializer<LocalDate>() {
    override fun serialize(value: LocalDate, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.toString())
    }
}

class BigDecimalSerializer : JsonSerializer<BigDecimal>() {
    override fun serialize(value: BigDecimal, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.setScale(2, RoundingMode.HALF_UP).toString())
    }
}

fun configureObjectMapper(objectMapper: ObjectMapper): ObjectMapper {
    val module = SimpleModule()
    module.addSerializer(BigDecimal::class.java, BigDecimalSerializer())
    module.addSerializer(LocalDate::class.java, LocalDateSerializer())
    objectMapper.registerModule(module)
    return objectMapper
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class TransactionControllerTest(
    @LocalServerPort val port: Int,
    @Autowired val transactionStateTestAdapter: TransactionStateTestAdapter,
    @Autowired private val accountStateTestAdapter: AccountStateTestAdapter,
    @Autowired var objectMapper: ObjectMapper,
    @Autowired val tokenGenerator: TokenGenerator,
    @Autowired private val transactionJpaRepository: TransactionJpaRepository
): AuthenticatedUserTest() {



    @BeforeEach
    fun setup() {
        configureObjectMapper(objectMapper)
    }

    @AfterEach
    fun clear() {
        accountStateTestAdapter.clear()
        transactionStateTestAdapter.clear()
    }

    @Nested
    inner class CreateTransactionEndpointTest {

        @Test
        fun `Create a transaction must send 200`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val body = UserBookletResponse("test", TransactionResult(null, "transactionTest", BigDecimal(100.00), "€", true, LocalDate.now(), null, false))

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/transaction")
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("transactionTest"),
                    "value", equalTo("100.00")
                )
            }
        }

        @Test
        fun `Create a transaction with an unknown account must send 404`() {
            val body = UserBookletResponse("test", TransactionResult(null, "transactionTest", BigDecimal(100.00), "€", true, LocalDate.now(), null, false))

            Given {
                port(port)
                cookie(generateCookie(token))
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/transaction")
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Create a transaction with an unauthenticated user must send 404`() {
            val body = UserBookletResponse("test", TransactionResult(null, "transactionTest", BigDecimal(100.00), "€", true, LocalDate.now(), null, false))

            Given {
                port(port)
                cookie(generateCookie(tokenGenerator.generateToken(UserId(UUID.randomUUID()), "test", setOf(Role.USER)).tokenValue))
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/transaction")
            } Then {
                statusCode(404)
            }
        }
    }

    @Nested
    inner class FindTransactionByIdEndpointTest {

        @Test
        fun `Find a transaction with its id must send 200 and the asked transaction`() {
            // When
            val element = Booklet(200.00.toAmount(), "test", owner = user)
            accountStateTestAdapter.init(
                listOf(element)
            )
            val justInputAccount = accountStateTestAdapter.get().find { it.label == "test" }!!
            transactionStateTestAdapter.init(listOf(
                AccountTransaction(user!!.id, justInputAccount.label, listOf(Transaction(null, "testTransaction", LocalDate.now(), Amount(200.00.toBigDecimal()), false)), token.asTokenUUID())
            ))
            val justInputTransaction = transactionStateTestAdapter.get().find { it.label == "testTransaction" }!!

            // When

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                param("userID", user!!.id.value)
            } When {
                get("/api/transaction/{id}", mapOf("id" to justInputTransaction.id))
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("testTransaction"),
                    "isIncome", equalTo(false)
                )
            }
        }

        @Test
        fun `Request for an unknown transaction must send 404`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                param("userID", user!!.id.value)
            } When {
                get("/api/transaction/{id}", mapOf("id" to "12"))
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Request for an unauthenticated user must send 404`() {
            Given {
                port(port)
                cookie(generateCookie(tokenGenerator.generateToken(UserId(UUID.randomUUID()), "test", setOf(Role.USER)).tokenValue))
                header("Content-Type", "application/json")
                param("userID", "2")
            } When {
                get("/api/transaction/{id}", mapOf("id" to "12"))
            } Then {
                statusCode(404)
            }
        }
    }
    @Nested
    inner class RequestForTransactionsByDate {
        @Test
        fun `Request for transactions for a certain month and year must return 200 with all the requested ones and only those`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val transactions = listOf(
                Transaction(null, "test1", LocalDate.of(2024, Month.JUNE, 1), Amount.fromString("100.00"), false),
                Transaction(null, "test2", LocalDate.of(2024, Month.JUNE, 2), Amount.fromString("50.00"), true),
                Transaction(null, "test3", LocalDate.of(2024, Month.JUNE, 5), Amount.fromString("300.00"), false),
                Transaction(null, "test4", LocalDate.of(2024, Month.JUNE, 4), Amount.fromString("10050.00"), true),
                Transaction(null, "test5", LocalDate.of(2024, Month.JUNE, 20), Amount.fromString("100.00"), false),
                Transaction(null, "test6", LocalDate.of(2024, Month.MAY, 20), Amount.fromString("100.00"), false),
            )
            transactionStateTestAdapter.init(
                listOf(
                    AccountTransaction(
                        user!!.id,
                        "test",
                        transactions,
                        token.asTokenUUID()
                    )
                )
            )
            val booklet = accountStateTestAdapter.get().find { it.label == "test" }!!
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")

                param("userId", user!!.id.value)
                param("month", Month.JUNE)
                param("year", 2024)
                param("bookletId", booklet.id)
            } When {
                get("/api/transaction")
            } Then {
                statusCode(200)
                body(
                    "transactions.label", hasItem("test1"),
                    "transactions.label", hasItem("test2"),
                    "transactions.label", hasItem("test3"),
                    "transactions.label", hasItem("test4"),
                    "transactions.label", hasItem("test5"),
                    "transactions.label", not(hasItem("test6"))
                )
            }
        }
    }

    @Nested
    inner class DeleteTransactionEndpointTest {
        @Test
        fun `delete an existing transaction should return 200`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val transactions = listOf(
                Transaction(null, "test1", LocalDate.of(2024, Month.JUNE, 1), Amount.fromString("100.00"), false),
                Transaction(null, "test2", LocalDate.of(2024, Month.JUNE, 2), Amount.fromString("50.00"), true),
                Transaction(null, "test3", LocalDate.of(2024, Month.JUNE, 5), Amount.fromString("300.00"), false),
                Transaction(null, "test4", LocalDate.of(2024, Month.JUNE, 4), Amount.fromString("10050.00"), true),
                Transaction(null, "test5", LocalDate.of(2024, Month.JUNE, 20), Amount.fromString("100.00"), false),
                Transaction(null, "test6", LocalDate.of(2024, Month.MAY, 20), Amount.fromString("100.00"), false),
            )
            val account = accountStateTestAdapter.get().find { it.label == "test" }!!
            transactionStateTestAdapter.init(
                listOf(
                    AccountTransaction(
                        user!!.id,
                        "test",
                        transactions,
                        token.asTokenUUID()
                    )
                )
            )
            val ids = transactionStateTestAdapter.get().mapNotNull { it.id }
            val request = AccountTransactionsIdRequest(
                account.id!!.toString(),
                ids.map { it.toString() }
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(request))
            } When {
                delete("/api/transaction")
            } Then {
                statusCode(200)
                body("deletedIds.size()", equalTo(ids.size))
                body("amount", notNullValue())
            }

            assertEquals(0, transactionStateTestAdapter.get().size)
        }
        @Test
        fun `Request deletion for an non-existing account must send 404`() {
            val request = AccountTransactionsIdRequest(
                UUID.randomUUID().toString(),
                listOf()
            )
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(request))
            } When {
                delete("/api/transaction")
            } Then {
                statusCode(404)
            }
        }
        @Test
        fun `delete should remove targeted transaction from database`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )

            val transactions = listOf(
                Transaction(null, "to-delete", LocalDate.of(2024, Month.JUNE, 1), Amount.fromString("100.00"), false),
                Transaction(null, "to-keep", LocalDate.of(2024, Month.JUNE, 2), Amount.fromString("50.00"), true)
            )

            transactionStateTestAdapter.init(
                listOf(
                    AccountTransaction(
                        user!!.id,
                        "test",
                        transactions,
                        token.asTokenUUID()
                    )
                )
            )

            val account = accountStateTestAdapter.get().find { it.label == "test" }!!
            val transactionToDelete = transactionStateTestAdapter.get().first { it.label == "to-delete" }
            val transactionToKeep = transactionStateTestAdapter.get().first { it.label == "to-keep" }

            assertNotNull(transactionJpaRepository.findSheetResourceByIdSheet(transactionToDelete.id!!))
            assertNotNull(transactionJpaRepository.findSheetResourceByIdSheet(transactionToKeep.id!!))

            val request = AccountTransactionsIdRequest(
                account.id!!.toString(),
                listOf(transactionToDelete.id!!.toString())
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(request))
            } When {
                delete("/api/transaction")
            } Then {
                statusCode(200)
                body("deletedIds.size()", equalTo(1))
            }

            assertNull(transactionJpaRepository.findSheetResourceByIdSheet(transactionToDelete.id!!))
            assertNotNull(transactionJpaRepository.findSheetResourceByIdSheet(transactionToKeep.id!!))
        }
    }
    @Nested
    inner class PatchTransactionEndpointTest {
        @Test
        fun `Update an existing transaction should return 200 and the updated one`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val transactions = listOf(
                Transaction(null, "test1", LocalDate.of(2024, Month.JUNE, 1), Amount.fromString("100.00"), false),
                Transaction(null, "test2", LocalDate.of(2024, Month.JUNE, 2), Amount.fromString("50.00"), true),
                Transaction(null, "test3", LocalDate.of(2024, Month.JUNE, 5), Amount.fromString("300.00"), false),
                Transaction(null, "test4", LocalDate.of(2024, Month.JUNE, 4), Amount.fromString("10050.00"), true),
                Transaction(null, "test5", LocalDate.of(2024, Month.JUNE, 20), Amount.fromString("100.00"), false),
                Transaction(null, "test6", LocalDate.of(2024, Month.MAY, 20), Amount.fromString("100.00"), false),
            )
            transactionStateTestAdapter.init(
                listOf(
                    AccountTransaction(user!!.id, "test", transactions, token.asTokenUUID())
                )
            )

            val account = accountStateTestAdapter.get().find { it.label == "test" }!!
            val accountId = account.id
            val transactionToPatch = transactionStateTestAdapter.get()
                .find { it.label == "test2" }!!
            val body = UserAccountIdsTransactionRequest(
                accountId = accountId!!.toString(),
                transaction = transactionToPatch.toDTO()
                    .copy(
                        label = "test4",
                        value = BigDecimal(150)
                    )
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                patch("/api/transaction")
            } Then {
                statusCode(200)
            }
        }
    }
    @Nested
    inner class ConfirmPreviewTransactionEndpointTest {
        @Test
        fun `Request to confirm a preview transaction`() {
            accountStateTestAdapter.init(
                listOf(Booklet(200.toAmount(), "test", owner = user))
            )
            val transactions = listOf(
                Transaction(null, "test1", LocalDate.of(2024, Month.JUNE, 1), Amount.fromString("100.00"), false, isPreview = true),
                Transaction(null, "test2", LocalDate.of(2024, Month.JUNE, 2), Amount.fromString("50.00"), true, isPreview = true),
                Transaction(null, "test3", LocalDate.of(2024, Month.JUNE, 5), Amount.fromString("300.00"), false, isPreview = true),
                Transaction(null, "test4", LocalDate.of(2024, Month.JUNE, 4), Amount.fromString("10050.00"), true),
                Transaction(null, "test5", LocalDate.of(2024, Month.JUNE, 20), Amount.fromString("100.00"), false),
                Transaction(null, "test6", LocalDate.of(2024, Month.MAY, 20), Amount.fromString("100.00"), false),
            )
            transactionStateTestAdapter.init(
                listOf(
                    AccountTransaction(
                        user!!.id,
                        "test",
                        transactions,
                        token.asTokenUUID()
                    )
                )
            )
            val account = accountStateTestAdapter.get().find { it.label == "test" }!!
            val transaction = transactionStateTestAdapter.get().find { it.label == "test2" }!!

            val body = ConfirmPreviewRequest(
                account.id!!.toString(),
                transaction.id!!.toString(),
                null
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                patch("/api/transaction/confirm")
            } Then {
                statusCode(200)
            }
        }
    }
}