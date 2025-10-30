package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.FeatureTest
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CsvImportFeatureTest : FeatureTest() {

    private val csvImportFeature = FakeFactory.csvImportFeature

    @Test
    fun `importTransactionsFromCsv should fail when user is not authenticated`() {
        val invalidToken = "invalid-token"

        val result = csvImportFeature.importTransactionsFromCsv(
            invalidToken,
            java.util.UUID.randomUUID(),
            "csv content"
        )

        assertTrue(result.isFailure())
        assertEquals(ResultState.UNAUTHORIZED, result.status)
    }

    @Test
    fun `importTransactionsFromCsv should fail when booklet does not exist`() {
        launchWithConnectedUserInstance {
            val nonExistentBookletId = java.util.UUID.randomUUID()

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                nonExistentBookletId,
                "csv content"
            )

            assertTrue(result.isFailure())
            assertEquals(ResultState.NOT_FOUND, result.status)
        }
    }

    @Test
    fun `importTransactionsFromCsv should fail when user does not own the booklet`() {
        launchWithConnectedUserInstance {
            val otherUser = createAccount(
                fr.sacane.jmanager.domain.models.User(
                    fr.sacane.jmanager.domain.models.UserId(java.util.UUID.randomUUID()),
                    "otherUser",
                    "other@test.fr"
                ),
                "other account",
                100.toAmount()
            )

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                otherUser.id!!,
                "csv content"
            )

            assertTrue(result.isFailure())
            assertEquals(ResultState.FORBIDDEN, result.status)
        }
    }

    @Test
    fun `importTransactionsFromCsv should fail when CSV is empty`() {
        launchWithConnectedUserInstance {
            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                ""
            )

            assertTrue(result.isFailure())
            assertEquals(ResultState.INVALID, result.status)
            assertTrue(result.message.contains("empty"))
        }
    }

    @Test
    fun `importTransactionsFromCsv should fail when CSV header is invalid`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,wrong_column,recette,tag
                15-01-2025,Test,10.00,,
            """.trimIndent()

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isFailure())
            assertEquals(ResultState.INVALID, result.status)
            assertTrue(result.message.contains("header"))
        }
    }

    @Test
    fun `importTransactionsFromCsv should successfully import valid transactions`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Courses,45.50,,
                20-01-2025,Salaire,,2500.00,
            """.trimIndent()

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                assertEquals(2, importResult.successCount)
                assertEquals(0, importResult.failedLines.size)
                assertEquals(2, importResult.transactions.size)
                assertFalse(importResult.hasErrors)

                val transaction1 = importResult.transactions.find { it.label == "Courses" }
                assertNotNull(transaction1)
                assertEquals(java.math.BigDecimal("45.50"), transaction1!!.amount.value)
                assertFalse(transaction1.isIncome)

                val transaction2 = importResult.transactions.find { it.label == "Salaire" }
                assertNotNull(transaction2)
                assertEquals(java.math.BigDecimal("2500.00"), transaction2!!.amount.value)
                assertTrue(transaction2.isIncome)
            }
        }
    }

    @Test
    fun `importTransactionsFromCsv should return errors for invalid lines`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Valid transaction,45.50,,
                invalid-date,Invalid transaction,50.00,,
                ,Missing date,30.00,,
            """.trimIndent()

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                assertEquals(1, importResult.successCount)
                assertEquals(2, importResult.failedLines.size)
                assertTrue(importResult.hasErrors)

                val errorLineNumbers = importResult.failedLines.map { it.lineNumber }
                assertTrue(errorLineNumbers.contains(3))
                assertTrue(errorLineNumbers.contains(4))
            }
        }
    }

    @Test
    fun `importTransactionsFromCsv should skip lines with wrong column count`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Valid transaction,45.50,,
                20-01-2025,Missing columns,50.00
            """.trimIndent()

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                assertEquals(1, importResult.successCount)
            }
        }
    }

    @Test
    fun `importTransactionsFromCsv should match tags case insensitively`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Courses,45.50,,alimentation & restaurant
            """.trimIndent()

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                assertEquals(1, importResult.successCount)
                assertEquals(0, importResult.failedLines.size)

                val transaction = importResult.transactions.first()
                assertEquals("Alimentation & Restaurant", transaction.tag?.label)
            }
        }
    }

    @Test
    fun `importTransactionsFromCsv should handle both comma and dot as decimal separator`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,With comma,"45,50",,
                16-01-2025,With dot,67.80,,
            """.trimIndent()

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                assertEquals(2, importResult.successCount)
                assertEquals(0, importResult.failedLines.size)
            }
        }
    }

    @Test
    fun `importTransactionsFromCsv should validate that either depense or recette is filled`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Both filled,10.00,20.00,
                16-01-2025,None filled,,,,
            """.trimIndent()

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                assertEquals(0, importResult.successCount)
                assertEquals(2, importResult.failedLines.size)
            }
        }
    }

    @Test
    fun `importTransactionsFromCsv should use default tag when tag is not found`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Test,10.00,,NonExistentTag
            """.trimIndent()

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                assertEquals(1, importResult.successCount)
                val transaction = importResult.transactions.first()
                assertEquals("Aucune", transaction.tag?.label)
            }
        }
    }
}

