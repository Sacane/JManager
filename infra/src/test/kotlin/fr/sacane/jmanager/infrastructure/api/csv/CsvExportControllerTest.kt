package fr.sacane.jmanager.infrastructure.api.csv

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import fr.sacane.jmanager.infrastructure.api.setup.AccountTransaction
import fr.sacane.jmanager.infrastructure.api.setup.TransactionStateTestAdapter
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.module.kotlin.extensions.Extract
import org.hamcrest.CoreMatchers.*
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
@DisplayName("CSV Export Controller Tests")
class CsvExportControllerTest(
    @param:LocalServerPort val port: Int,
    @param:Autowired val accountStateAdapter: AccountStateTestAdapter,
    @param:Autowired val transactionStateAdapter: TransactionStateTestAdapter,
    @param:Autowired val objectMapper: ObjectMapper
) : AuthenticatedUserTest() {

    private lateinit var bookletId: String
    private val testTransactions = mutableListOf<Transaction>()

    @BeforeEach
    fun setupBooklet() {
        accountStateAdapter.init(
            listOf(
                Booklet(
                    id = null,
                    amount = Amount.fromString("1000.00"),
                    labelAccount = "Test Booklet",
                    owner = user,
                )
            )
        )
        bookletId = accountStateAdapter.get().first().id!!.toString()

        val alimentationTag = Tag("Alimentation & Restaurant", isDefault = true)
        val transportTag = Tag("Transport", isDefault = true)

        testTransactions.clear()
        testTransactions.addAll(listOf(
            Transaction(
                id = null,
                label = "Courses alimentaires",
                date = LocalDate.of(2025, 1, 15),
                amount = Amount(BigDecimal("45.50")),
                isIncome = false,
                tag = alimentationTag
            ),
            Transaction(
                id = null,
                label = "Salaire",
                date = LocalDate.of(2025, 1, 20),
                amount = Amount(BigDecimal("2500.00")),
                isIncome = true,
                tag = null
            ),
            Transaction(
                id = null,
                label = "Essence",
                date = LocalDate.of(2025, 1, 22),
                amount = Amount(BigDecimal("60.00")),
                isIncome = false,
                tag = transportTag
            ),
            Transaction(
                id = null,
                label = "Transaction prévisionnelle",
                date = LocalDate.of(2025, 2, 15),
                amount = Amount(BigDecimal("100.00")),
                isIncome = false,
                isPreview = true,
                tag = null
            )
        ))

        transactionStateAdapter.init(
            listOf(
                AccountTransaction(
                    accountOwnerId = user?.id ?: error("User not initialized"),
                    accountName = "Test Booklet",
                    transactions = testTransactions,
                    token = token
                )
            )
        )
    }

    @AfterEach
    fun clear() {
        transactionStateAdapter.clear()
        accountStateAdapter.clear()
    }

    @Nested
    @DisplayName("POST /api/csv/export - Export Tests")
    inner class ExportCsvTests {

        @Test
        @DisplayName("Should export transactions to CSV successfully")
        fun `should export transactions to CSV successfully`() {
            val savedTransactions = transactionStateAdapter.get().filter { !it.isPreview }
            val transactionIds = savedTransactions.map { it.id.toString() }

            val requestBody = mapOf("transactionIds" to transactionIds)

            val response = Given {
                port(port)
                cookie("token", token)
                contentType(ContentType.JSON)
                body(objectMapper.writeValueAsString(requestBody))
            } When {
                post("/api/csv/export")
            } Then {
                statusCode(200)
                contentType(startsWith("text/csv"))
                header("Content-Disposition", containsString("attachment"))
            } Extract {
                asString()
            }

            val lines = response.split("\n")
            Assertions.assertTrue(lines.isNotEmpty())
            Assertions.assertEquals("date;label;depense;recette;tag", lines[0])
            Assertions.assertEquals(4, lines.size)
        }

        @Test
        @DisplayName("Should not export preview transactions")
        fun `should not export preview transactions`() {
            val allTransactions = transactionStateAdapter.get()
            val transactionIds = allTransactions.map { it.id.toString() }

            val requestBody = mapOf("transactionIds" to transactionIds)

            val response = Given {
                port(port)
                cookie("token", token)
                contentType(ContentType.JSON)
                body(objectMapper.writeValueAsString(requestBody))
            } When {
                post("/api/csv/export")
            } Then {
                statusCode(200)
            } Extract {
                asString()
            }

            val lines = response.split("\n")
            Assertions.assertEquals(4, lines.size)
            Assertions.assertFalse(response.contains("Transaction prévisionnelle"))
        }

        @Test
        @DisplayName("Should return 400 when transaction list is empty")
        fun `should return 400 when transaction list is empty`() {
            val requestBody = mapOf("transactionIds" to emptyList<String>())

            Given {
                port(port)
                cookie("token", token)
                contentType(ContentType.JSON)
                body(objectMapper.writeValueAsString(requestBody))
            } When {
                post("/api/csv/export")
            } Then {
                statusCode(400)
                body("message", containsString("ne peut pas être vide"))
            }
        }

        @Test
        @DisplayName("Should return 400 when transaction list exceeds maximum")
        fun `should return 400 when transaction list exceeds maximum`() {
            val tooManyIds = (1..10001).map { UUID.randomUUID().toString() }
            val requestBody = mapOf("transactionIds" to tooManyIds)

            Given {
                port(port)
                cookie("token", token)
                contentType(ContentType.JSON)
                body(objectMapper.writeValueAsString(requestBody))
            } When {
                post("/api/csv/export")
            } Then {
                statusCode(400)
                body("message", containsString("ne peut pas dépasser 10 000"))
            }
        }

        @Test
        @DisplayName("Should return 404 when no transactions found")
        fun `should return 404 when no transactions found`() {
            val nonExistentIds = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString())
            val requestBody = mapOf("transactionIds" to nonExistentIds)

            Given {
                port(port)
                cookie("token", token)
                contentType(ContentType.JSON)
                body(objectMapper.writeValueAsString(requestBody))
            } When {
                post("/api/csv/export")
            } Then {
                statusCode(404)
                body("message", containsString("Aucune transaction trouvée"))
            }
        }

        @Test
        @DisplayName("Should return 400 when transaction IDs are invalid")
        fun `should return 400 when transaction IDs are invalid`() {
            val invalidIds = listOf("invalid-uuid", "not-a-uuid", "123")
            val requestBody = mapOf("transactionIds" to invalidIds)

            Given {
                port(port)
                cookie("token", token)
                contentType(ContentType.JSON)
                body(objectMapper.writeValueAsString(requestBody))
            } When {
                post("/api/csv/export")
            } Then {
                statusCode(400)
                body("message", containsString("Aucun ID de transaction valide fourni"))
            }
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        fun `should return 401 when user is not authenticated`() {
            val requestBody = mapOf("transactionIds" to listOf(UUID.randomUUID().toString()))

            Given {
                port(port)
                contentType(ContentType.JSON)
                body(objectMapper.writeValueAsString(requestBody))
            } When {
                post("/api/csv/export")
            } Then {
                // Spring Security peut retourner 401 (Unauthorized) ou 403 (Forbidden)
                statusCode(anyOf(equalTo(401), equalTo(403)))
            }
        }
    }

    @Nested
    @DisplayName("Security Tests")
    inner class SecurityTests {

        @Test
        @DisplayName("Should prevent SQL injection in transaction IDs")
        fun `should prevent SQL injection in transaction IDs`() {
            val sqlInjectionAttempts = listOf(
                "1' OR '1'='1",
                "1; DROP TABLE transactions--",
                "1 UNION SELECT * FROM users--"
            )
            val requestBody = mapOf("transactionIds" to sqlInjectionAttempts)

            Given {
                port(port)
                cookie("token", token)
                contentType(ContentType.JSON)
                body(objectMapper.writeValueAsString(requestBody))
            } When {
                post("/api/csv/export")
            } Then {
                statusCode(400)
                body("message", containsString("Aucun ID de transaction valide fourni"))
            }
        }

        @Test
        @DisplayName("Should handle very long transaction ID list gracefully")
        fun `should handle very long transaction ID list gracefully`() {
            val longList = (1..5000).map { UUID.randomUUID().toString() }
            val requestBody = mapOf("transactionIds" to longList)

            Given {
                port(port)
                cookie("token", token)
                contentType(ContentType.JSON)
                body(objectMapper.writeValueAsString(requestBody))
            } When {
                post("/api/csv/export")
            } Then {
                statusCode(anyOf(equalTo(404), equalTo(200)))
            }
        }
    }
}

