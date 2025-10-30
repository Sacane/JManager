package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.FeatureTest
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("CsvImportFeature Tests")
class CsvImportFeatureTest : FeatureTest() {

    private val csvImportFeature = FakeFactory.csvImportFeature

    @Test
    @DisplayName("Should fail when user is not authenticated")
    fun `validateCsvFile should fail when user is not authenticated`() {
        val invalidToken = "invalid-token"

        val result = csvImportFeature.validateCsvFile(
            invalidToken,
            java.util.UUID.randomUUID(),
            "csv content"
        )

        assertTrue(result.isFailure())
        assertEquals(ResultState.UNAUTHORIZED, result.status)
    }

    @Test
    @DisplayName("Should fail when booklet does not exist")
    fun `validateCsvFile should fail when booklet does not exist`() {
        launchWithConnectedUserInstance {
            val nonExistentBookletId = java.util.UUID.randomUUID()

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                nonExistentBookletId,
                "csv content"
            )

            assertTrue(result.isFailure())
            assertEquals(ResultState.NOT_FOUND, result.status)
        }
    }

    @Test
    @DisplayName("Should fail when user does not own the booklet")
    fun `validateCsvFile should fail when user does not own the booklet`() {
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

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                otherUser.id!!,
                "csv content"
            )

            assertTrue(result.isFailure())
            assertEquals(ResultState.FORBIDDEN, result.status)
        }
    }

    @Test
    @DisplayName("Should return report with error when CSV is empty")
    fun `validateCsvFile should return report with error when CSV is empty`() {
        launchWithConnectedUserInstance {
            val csvContent = ""

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { report ->
                assertTrue(report.hasErrors)
                assertFalse(report.canImport)
            }
        }
    }

    @Test
    @DisplayName("Should return report with error when CSV header is invalid")
    fun `validateCsvFile should return report with error when CSV header is invalid`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,wrong_column,recette,tag
                15-01-2025,Test,10.00,,
            """.trimIndent()

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { report ->
                assertTrue(report.hasErrors)
                assertFalse(report.canImport)
            }
        }
    }

    @Test
    @DisplayName("Should return report with error when line has invalid date format")
    fun `validateCsvFile should return report with error when line has invalid date format`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                2025-01-15,Test,10.00,,
            """.trimIndent()

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { report ->
                assertTrue(report.hasErrors)
                assertFalse(report.canImport)
            }
        }
    }

    @Test
    @DisplayName("Should succeed validation with valid CSV and no warnings")
    fun `validateCsvFile should succeed with valid CSV`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Groceries,45.50,,Alimentation & Restaurant
                16-01-2025,Salary,,2500.00,Aucune
            """.trimIndent()

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { report ->
                assertFalse(report.hasErrors)
                assertTrue(report.canImport)
                assertEquals(2, report.totalLines)
                assertEquals(2, report.validLines)
                assertEquals(0, report.errors.size)
                assertEquals(0, report.warnings.size)
            }
        }
    }

    @Test
    @DisplayName("Should succeed validation with warnings for unknown tags")
    fun `validateCsvFile should succeed with warnings when tag is unknown`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Groceries,45.50,,UnknownTag
            """.trimIndent()

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { report ->
                assertFalse(report.hasErrors)
                assertTrue(report.canImport)
                assertEquals(1, report.totalLines)
                assertEquals(1, report.validLines)
                assertEquals(0, report.errors.size)
                assertEquals(1, report.warnings.size)
            }
        }
    }

    @Test
    @DisplayName("Should return report with error when both amounts are filled")
    fun `validateCsvFile should return report with error when both depense and recette are filled`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Test,45.50,100.00,
            """.trimIndent()

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { report ->
                assertTrue(report.hasErrors)
                assertFalse(report.canImport)
            }
        }
    }

    @Test
    @DisplayName("Should return report with error when no amount is filled")
    fun `validateCsvFile should return report with error when neither amount is filled`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Test,,,
            """.trimIndent()

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { report ->
                assertTrue(report.hasErrors)
                assertFalse(report.canImport)
            }
        }
    }

    @Test
    @DisplayName("Should return report with error when amount is negative")
    fun `validateCsvFile should return report with error when amount is negative`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Test,-45.50,,
            """.trimIndent()

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { report ->
                assertTrue(report.hasErrors)
                assertFalse(report.canImport)
            }
        }
    }

    @Test
    @DisplayName("Should accept comma as decimal separator")
    fun `validateCsvFile should accept comma as decimal separator`() {
        launchWithConnectedUserInstance {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Test,\"45,50\",,\n"

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { report ->
                assertFalse(report.hasErrors)
                assertTrue(report.canImport)
            }
        }
    }

    @Test
    @DisplayName("Should return report with error when columns have wrong count")
    fun `validateCsvFile should return report with error when line has wrong column count`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Test,45.50
            """.trimIndent()

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { report ->
                assertTrue(report.hasErrors)
                assertFalse(report.canImport)
            }
        }
    }

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
}

