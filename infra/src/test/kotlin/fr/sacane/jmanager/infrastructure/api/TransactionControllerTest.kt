package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Transaction
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.AccountTransaction
import fr.sacane.jmanager.infrastructure.api.setup.TransactionStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.transaction.AccountTransactionsIdRequest
import fr.sacane.jmanager.infrastructure.api.transaction.TransactionResult
import fr.sacane.jmanager.infrastructure.api.transaction.UserBookletResponse
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.time.Month

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class TransactionControllerTest(
    @LocalServerPort val port: Int,
    @Autowired val transactionStateTestAdapter: TransactionStateTestAdapter,
    @Autowired private val accountStateTestAdapter: AccountStateTestAdapter,
    @Autowired val objectMapper: ObjectMapper
): AuthenticatedUserTest() {

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
                listOf(Account(200.toAmount(), "test", owner = user))
            )
            val body = UserBookletResponse(user!!.id.value!!, "test", TransactionResult(null, "transactionTest", "100.00", "€", true, LocalDate.now(), null, false))

            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/transaction")
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("transactionTest"),
                    "value", equalTo("100.00 €")
                )
            }
        }

        @Test
        fun `Create a transaction with an unknown account must send 404`() {
            val body = UserBookletResponse(user!!.id.value!!, "test", TransactionResult(null, "transactionTest", "100.00", "€", true, LocalDate.now(), null, false))

            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/transaction")
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Create a transaction with an unauthenticated user must send 401`() {
            val body = UserBookletResponse(101, "test", TransactionResult(null, "transactionTest", "100.00", "€", true, LocalDate.now(), null, false))

            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/transaction")
            } Then {
                statusCode(401)
            }
        }
    }

    @Nested
    inner class FindTransactionByIdEndpointTest {

        @Test
        fun `Find a transaction with its id must send 200 and the asked transaction`() {
            // When
            val element = Account(200.toAmount(), "test", owner = user)
            accountStateTestAdapter.init(
                listOf(element)
            )
            val justInputAccount = accountStateTestAdapter.get().find { it.label == "test" }!!
            transactionStateTestAdapter.init(listOf(
                AccountTransaction(user!!.id, justInputAccount.label, listOf(Transaction(null, "testTransaction", LocalDate.now(), Amount.fromString("200"), false)), token.asTokenUUID())
            ))
            val justInputTransaction = transactionStateTestAdapter.get().find { it.label == "testTransaction" }!!

            // When

            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
                param("userID", user!!.id.value)
            } When {
                get("/api/transaction/{id}", mapOf("id" to justInputTransaction.id))
            } Then {
                statusCode(200)
                body(
                    "label", equalTo("testTransaction"),
                    "value", equalTo("200.00"),
                    "isIncome", equalTo(false)
                )
            }
        }

        @Test
        fun `Request for an unknown transaction must send 404`() {
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
                param("userID", user!!.id.value)
            } When {
                get("/api/transaction/{id}", mapOf("id" to "12"))
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `Request for an unauthenticated user must send 401`() {
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
                param("userID", "2")
            } When {
                get("/api/transaction/{id}", mapOf("id" to "12"))
            } Then {
                statusCode(401)
            }
        }
    }
    @Nested
    inner class RequestForTransactionsByDate {
        @Test
        fun `Request for transactions for a certain month and year must return 200 with all the requested ones and only those`() {
            accountStateTestAdapter.init(
                listOf(Account(200.toAmount(), "test", owner = user))
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
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")

                param("userId", user!!.id.value)
                param("month", Month.JUNE)
                param("year", 2024)
                param("accountLabel", "test")
            } When {
                get("/api/transaction")
            } Then {
                statusCode(200)
                body(
                    "sheets.label", hasItem("test1"),
                    "sheets.label", hasItem("test2"),
                    "sheets.label", hasItem("test3"),
                    "sheets.label", hasItem("test4"),
                    "sheets.label", hasItem("test5"),
                    "sheets.label", not(hasItem("test6"))
                )
            }
        }
    }

    @Nested
    inner class DeleteTransactionEndpointTest {
        @Test
        fun `delete an existing transaction should return 200`() {
            accountStateTestAdapter.init(
                listOf(Account(200.toAmount(), "test", owner = user))
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
                account.id!!,
                ids
            )
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(request))
            } When {
                delete("/api/transaction/{userId}", mapOf("userId" to user!!.id.value))
            } Then {
                statusCode(200)
            }

            assertEquals(0, transactionStateTestAdapter.get().size)
        }
        @Test
        fun `Request deletion for an non-existing account must send 404`() {
            val request = AccountTransactionsIdRequest(
                1029,
                listOf()
            )
            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(request))
            } When {
                delete("/api/transaction/{userId}", mapOf("userId" to user!!.id.value))
            } Then {
                statusCode(404)
            }
        }
    }
    @Nested
    inner class PatchTransactionEndpointTest {
//        @Test
//        fun `Update an existing transaction should return 200 and the updated one`() {
//            accountStateTestAdapter.init(
//                listOf(Account(200.toAmount(), "test", owner = user))
//            )
//            val transactions = listOf(
//                Transaction(null, "test1", LocalDate.of(2024, Month.JUNE, 1), Amount.fromString("100.00"), false),
//                Transaction(null, "test2", LocalDate.of(2024, Month.JUNE, 2), Amount.fromString("50.00"), true),
//                Transaction(null, "test3", LocalDate.of(2024, Month.JUNE, 5), Amount.fromString("300.00"), false),
//                Transaction(null, "test4", LocalDate.of(2024, Month.JUNE, 4), Amount.fromString("10050.00"), true),
//                Transaction(null, "test5", LocalDate.of(2024, Month.JUNE, 20), Amount.fromString("100.00"), false),
//                Transaction(null, "test6", LocalDate.of(2024, Month.MAY, 20), Amount.fromString("100.00"), false),
//            )
//            transactionStateTestAdapter.init(
//                listOf(
//                    AccountTransaction(user!!.id, "test", transactions, token.asTokenUUID())
//                )
//            )
//
//            Given {
//                port(port)
//                header("Authorization", token)
//                header("Content-Type", "application/json")
//            } When {
//                post("/api/transaction")
//            } Then {
//                statusCode(200)
//                body(
//                    "label", equalTo("transactionTest"),
//                    "value", equalTo("100.00 €")
//                )
//            }
//        }
    }

}