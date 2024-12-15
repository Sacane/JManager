package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.TransactionStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.tag.TagDTO
import fr.sacane.jmanager.infrastructure.api.transaction.SheetDTO
import fr.sacane.jmanager.infrastructure.api.transaction.UserAccountSheetDTO
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
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
    }
}