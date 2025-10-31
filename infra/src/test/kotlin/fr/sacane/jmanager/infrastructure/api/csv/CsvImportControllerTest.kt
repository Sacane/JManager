package fr.sacane.jmanager.infrastructure.api.csv

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.AccountStateTestAdapter
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.io.File
import java.nio.charset.StandardCharsets

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
@DisplayName("CSV Import Controller Tests")
class CsvImportControllerTest(
    @param:LocalServerPort val port: Int,
    @param:Autowired val accountStateAdapter: AccountStateTestAdapter,
    @param:Autowired val objectMapper: ObjectMapper
) : AuthenticatedUserTest() {

    private lateinit var bookletId: String

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
    }

    @AfterEach
    fun clear() {
        accountStateAdapter.clear()
    }

    private fun createTempCsvFile(content: String): File {
        val tempFile = File.createTempFile("test_csv_", ".csv")
        tempFile.writeText(content, StandardCharsets.UTF_8)
        tempFile.deleteOnExit()
        return tempFile
    }

    @Nested
    @DisplayName("POST /api/csv/validate/{bookletId} - Validation Tests")
    inner class ValidateCsvTests {

        @Test
        @DisplayName("Should return 404 when booklet does not exist")
        fun `should return 404 when booklet does not exist`() {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Test,45.50,,\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/validate/00000000-0000-0000-0000-000000000000")
            } Then {
                statusCode(404)
            }
        }

        @Test
        @DisplayName("Should return 200 with errors when CSV header is invalid")
        fun `should return 200 with errors when CSV header is invalid`() {
            val csvContent = "date,label,wrong_column,recette,tag\n15-01-2025,Test,45.50,,\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/validate/$bookletId")
            } Then {
                statusCode(200)
                body("hasErrors", equalTo(true))
                body("canImport", equalTo(false))
                body("errors.size()", greaterThan(0))
            }
        }

        @Test
        @DisplayName("Should return 200 with errors when date format is invalid")
        fun `should return 200 with errors when date format is invalid`() {
            val csvContent = "date,label,depense,recette,tag\n2025-01-15,Test,45.50,,\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/validate/$bookletId")
            } Then {
                statusCode(200)
                body("hasErrors", equalTo(true))
                body("canImport", equalTo(false))
            }
        }

        @Test
        @DisplayName("Should return 200 with errors when both amounts are filled")
        fun `should return 200 with errors when both depense and recette are filled`() {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Test,45.50,100.00,\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/validate/$bookletId")
            } Then {
                statusCode(200)
                body("hasErrors", equalTo(true))
                body("canImport", equalTo(false))
            }
        }

        @Test
        @DisplayName("Should return 200 with errors when no amount is filled")
        fun `should return 200 with errors when neither amount is filled`() {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Test,,,\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/validate/$bookletId")
            } Then {
                statusCode(200)
                body("hasErrors", equalTo(true))
                body("canImport", equalTo(false))
            }
        }

        @Test
        @DisplayName("Should return 200 with errors when amount is negative")
        fun `should return 200 with errors when amount is negative`() {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Test,-45.50,,\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/validate/$bookletId")
            } Then {
                statusCode(200)
                body("hasErrors", equalTo(true))
                body("canImport", equalTo(false))
            }
        }

        @Test
        @DisplayName("Should return 200 with errors when amount format is invalid")
        fun `should return 200 with errors when amount format is invalid`() {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Test,invalid,,\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/validate/$bookletId")
            } Then {
                statusCode(200)
                body("hasErrors", equalTo(true))
                body("canImport", equalTo(false))
            }
        }

        @Test
        @DisplayName("Should return 200 with validation report for valid CSV")
        fun `should return 200 with validation report for valid CSV`() {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Groceries,45.50,,Alimentation & Restaurant\n16-01-2025,Salary,,2500.00,Aucune\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/validate/$bookletId")
            } Then {
                statusCode(200)
                body("totalLines", equalTo(2))
                body("validLines", equalTo(2))
                body("hasErrors", equalTo(false))
                body("canImport", equalTo(true))
                body("errors.size()", equalTo(0))
                body("warnings.size()", equalTo(0))
            }
        }

        @Test
        @DisplayName("Should return 200 with warnings for unknown tag")
        fun `should return 200 with warnings when tag is unknown`() {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Groceries,45.50,,UnknownTag\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/validate/$bookletId")
            } Then {
                statusCode(200)
                body("totalLines", equalTo(1))
                body("validLines", equalTo(1))
                body("hasErrors", equalTo(false))
                body("canImport", equalTo(true))
                body("errors.size()", equalTo(0))
                body("warnings.size()", equalTo(1))
                body("warnings[0].lineNumber", equalTo(2))
                body("warnings[0].message", containsString("UnknownTag"))
            }
        }

        @Test
        @DisplayName("Should handle multiple lines with errors and warnings")
        fun `should return 200 with multiple errors and warnings`() {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Groceries,45.50,,UnknownTag1\ninvalid-date,Transport,20.00,,\n17-01-2025,Restaurant,35.00,,Alimentation & Restaurant\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/validate/$bookletId")
            } Then {
                statusCode(200)
                body("totalLines", equalTo(3))
                body("hasErrors", equalTo(true))
                body("canImport", equalTo(false))
                body("errors.size()", greaterThan(0))
            }
        }
    }

    @Nested
    @DisplayName("POST /api/csv/import/{bookletId} - Import Tests")
    inner class ImportCsvTests {

        @Test
        @DisplayName("Should return 404 when booklet does not exist")
        fun `should return 404 when booklet does not exist for import`() {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Test,45.50,,\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/import/00000000-0000-0000-0000-000000000000")
            } Then {
                statusCode(404)
            }
        }

        @Test
        @DisplayName("Should return 200 with import result for valid CSV")
        fun `should return 200 with import result for valid CSV`() {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Groceries,45.50,,Alimentation & Restaurant\n16-01-2025,Salary,,2500.00,Aucune\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/import/$bookletId")
            } Then {
                statusCode(200)
                body("successCount", equalTo(2))
                body("failedCount", equalTo(0))
                body("totalProcessed", equalTo(2))
                body("hasErrors", equalTo(false))
                body("transactions.size()", equalTo(2))
                body("errors.size()", equalTo(0))
            }
        }

        @Test
        @DisplayName("Should reject CSV with validation errors")
        fun `should reject CSV with validation errors`() {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Valid Transaction,45.50,,Alimentation & Restaurant\ninvalid-date,Invalid Transaction,30.00,,\n17-01-2025,Another Valid,25.00,,\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/import/$bookletId")
            } Then {
                statusCode(400)
                body("detail", containsString("CSV validation failed"))
                body("detail", containsString("Invalid date format"))
            }
        }

        @Test
        @DisplayName("Should import all valid transactions successfully")
        fun `should import all valid transactions when CSV is valid`() {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Transaction 1,45.50,,Alimentation & Restaurant\n16-01-2025,Transaction 2,30.00,,Transport\n17-01-2025,Transaction 3,25.00,,Aucune\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/import/$bookletId")
            } Then {
                statusCode(200)
                body("successCount", equalTo(3))
                body("failedCount", equalTo(0))
                body("totalProcessed", equalTo(3))
                body("hasErrors", equalTo(false))
                body("transactions.size()", equalTo(3))
            }
        }

        @Test
        @DisplayName("Should update booklet amount after importing expense transactions")
        fun `should update booklet amount correctly after importing expense transactions`() {
            // Récupérer le montant initial du livret
            val initialAmount = accountStateAdapter.get().first().amount

            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Groceries,45.50,,Alimentation & Restaurant\n16-01-2025,Transport,30.00,,Transport\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/import/$bookletId")
            } Then {
                statusCode(200)
                body("successCount", equalTo(2))
                body("failedCount", equalTo(0))
            }

            // Vérifier que le montant du livret a été mis à jour
            val updatedBooklet = accountStateAdapter.get().first()
            val expectedAmount = initialAmount.value.subtract(java.math.BigDecimal("75.50"))
            Assertions.assertEquals(expectedAmount, updatedBooklet.amount.value)
        }

        @Test
        @DisplayName("Should update booklet amount after importing income transactions")
        fun `should update booklet amount correctly after importing income transactions`() {
            // Récupérer le montant initial du livret
            val initialAmount = accountStateAdapter.get().first().amount

            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Salary,,2500.00,Aucune\n16-01-2025,Bonus,,500.00,Aucune\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/import/$bookletId")
            } Then {
                statusCode(200)
                body("successCount", equalTo(2))
                body("failedCount", equalTo(0))
            }

            // Vérifier que le montant du livret a été mis à jour
            val updatedBooklet = accountStateAdapter.get().first()
            val expectedAmount = initialAmount.value.add(java.math.BigDecimal("3000.00"))
            Assertions.assertEquals(expectedAmount, updatedBooklet.amount.value)
        }

        @Test
        @DisplayName("Should update booklet amount correctly with mixed transactions")
        fun `should update booklet amount correctly with mixed income and expense transactions`() {
            // Récupérer le montant initial du livret
            val initialAmount = accountStateAdapter.get().first().amount

            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Salary,,2500.00,Aucune\n16-01-2025,Groceries,45.50,,Alimentation & Restaurant\n17-01-2025,Transport,30.00,,Transport\n18-01-2025,Freelance,,800.00,Aucune\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/import/$bookletId")
            } Then {
                statusCode(200)
                body("successCount", equalTo(4))
                body("failedCount", equalTo(0))
            }

            // Vérifier que le montant du livret a été mis à jour
            // Recettes: 2500.00 + 800.00 = 3300.00
            // Dépenses: 45.50 + 30.00 = 75.50
            // Net: 3300.00 - 75.50 = 3224.50
            val updatedBooklet = accountStateAdapter.get().first()
            val expectedAmount = initialAmount.value.add(java.math.BigDecimal("3224.50"))
            Assertions.assertEquals(expectedAmount, updatedBooklet.amount.value)
        }

        @Test
        @DisplayName("Should not change booklet amount when import fails due to validation errors")
        fun `should not change booklet amount when import fails`() {
            // Récupérer le montant initial du livret
            val initialAmount = accountStateAdapter.get().first().amount

            val csvContent = "date,label,depense,recette,tag\ninvalid-date,Test,45.50,,\n"
            val file = createTempCsvFile(csvContent)

            Given {
                port(port)
                cookie("token", token)
                multiPart("file", file)
            } When {
                post("/api/csv/import/$bookletId")
            } Then {
                statusCode(400)
            }

            // Vérifier que le montant du livret n'a pas changé
            val updatedBooklet = accountStateAdapter.get().first()
            Assertions.assertEquals(initialAmount.value, updatedBooklet.amount.value)
        }
    }
}

