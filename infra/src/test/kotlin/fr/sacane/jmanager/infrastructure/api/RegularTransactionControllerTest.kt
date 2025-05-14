package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.RegularTransactionStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.TransactionStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.transaction.RegularTransactionCreationRequest
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.AfterEach
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class RegularTransactionControllerTest(
    @LocalServerPort val port: Int,
    @Autowired val transactionStateTestAdapter: RegularTransactionStateTestAdapter,
    @Autowired var objectMapper: ObjectMapper
): AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        transactionStateTestAdapter.clear()
    }

    @Nested
    inner class CreateRegularTransactionTest {

        @Test
        fun `should create a regular transaction`() {
            val body = RegularTransactionCreationRequest(
                startDate = LocalDate.now().toString(),
                label = "Test",
                value = BigDecimal(100.0),
                isIncome = true,
                regularity = "MONTHLY",
            )

            Given {
                port(port)
                cookie("token", token)
                contentType("application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                post("/api/transaction/regular")
            } Then {
                statusCode(200)
            }
            assertTrue {
                transactionStateTestAdapter.get().any { it.label == "Test" }
            }
        }
    }
}