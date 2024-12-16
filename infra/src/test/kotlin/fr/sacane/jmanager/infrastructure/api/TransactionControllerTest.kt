package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Transaction
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.AccountTransaction
import fr.sacane.jmanager.infrastructure.api.setup.TransactionStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.transaction.SheetDTO
import fr.sacane.jmanager.infrastructure.api.transaction.UserAccountSheetDTO
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class TransactionControllerTest(
    @LocalServerPort val port: Int,
    @Autowired val transactionStateTestAdapter: TransactionStateTestAdapter,
    @Autowired private val accountStateTestAdapter: AccountStateTestAdapter,
    @Autowired val objectMapper: ObjectMapper
): AsAuthenticatedUserTest() {

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
            val body = UserAccountSheetDTO(user!!.id.value!!, "test", SheetDTO(null, "transactionTest", "100.00", "€", true, LocalDate.now(), null, false))

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
            val body = UserAccountSheetDTO(user!!.id.value!!, "test", SheetDTO(null, "transactionTest", "100.00", "€", true, LocalDate.now(), null, false))

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
            val body = UserAccountSheetDTO(101, "test", SheetDTO(null, "transactionTest", "100.00", "€", true, LocalDate.now(), null, false))

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
                AccountTransaction(justInputAccount, listOf(Transaction(null, "testTransaction", LocalDate.now(), Amount.fromString("200"), false, )))
            ))

            // When

            Given {
                port(port)
                header("Authorization", token)
                header("Content-Type", "application/json")
                param("userID", user!!.id.value)
            } When {
                get("/api/transaction/{id}", mapOf("id" to justInputAccount.id))
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
}