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

    @Test
    @DisplayName("Should update booklet amount after importing expense transactions")
    fun `importTransactionsFromCsv should update booklet amount after importing expense transactions`() {
        launchWithConnectedUserInstance {
            val initialAmount = booklet.amount
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Groceries,45.50,,Alimentation & Restaurant
                16-01-2025,Transport,30.00,,Transport
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

                // Vérifier que le montant du livret a été mis à jour
                val accountState = FakeFactory.accountState()
                val updatedBooklets = accountState.getStates().find { it.userId == user.id }
                assertNotNull(updatedBooklets)
                val updatedBooklet = updatedBooklets!!.booklet.find { it.id == booklet.id }
                assertNotNull(updatedBooklet)

                // Le montant initial moins les dépenses (45.50 + 30.00 = 75.50)
                val expectedAmount = initialAmount.value.subtract(java.math.BigDecimal("75.50"))
                assertEquals(expectedAmount, updatedBooklet!!.amount.value)
            }
        }
    }

    @Test
    @DisplayName("Should update booklet amount after importing income transactions")
    fun `importTransactionsFromCsv should update booklet amount after importing income transactions`() {
        launchWithConnectedUserInstance {
            val initialAmount = booklet.amount
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Salary,,2500.00,Aucune
                16-01-2025,Bonus,,500.00,Aucune
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

                // Vérifier que le montant du livret a été mis à jour
                val accountState = FakeFactory.accountState()
                val updatedBooklets = accountState.getStates().find { it.userId == user.id }
                assertNotNull(updatedBooklets)
                val updatedBooklet = updatedBooklets!!.booklet.find { it.id == booklet.id }
                assertNotNull(updatedBooklet)

                // Le montant initial plus les recettes (2500.00 + 500.00 = 3000.00)
                val expectedAmount = initialAmount.value.add(java.math.BigDecimal("3000.00"))
                assertEquals(expectedAmount, updatedBooklet!!.amount.value)
            }
        }
    }

    @Test
    @DisplayName("Should update booklet amount correctly with mixed transactions")
    fun `importTransactionsFromCsv should update booklet amount with mixed income and expense transactions`() {
        launchWithConnectedUserInstance {
            val initialAmount = booklet.amount
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Salary,,2500.00,Aucune
                16-01-2025,Groceries,45.50,,Alimentation & Restaurant
                17-01-2025,Transport,30.00,,Transport
                18-01-2025,Freelance,,800.00,Aucune
            """.trimIndent()

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                assertEquals(4, importResult.successCount)
                assertEquals(0, importResult.failedLines.size)

                // Vérifier que le montant du livret a été mis à jour
                val accountState = FakeFactory.accountState()
                val updatedBooklets = accountState.getStates().find { it.userId == user.id }
                assertNotNull(updatedBooklets)
                val updatedBooklet = updatedBooklets!!.booklet.find { it.id == booklet.id }
                assertNotNull(updatedBooklet)

                // Recettes: 2500.00 + 800.00 = 3300.00
                // Dépenses: 45.50 + 30.00 = 75.50
                // Net: 3300.00 - 75.50 = 3224.50
                val expectedAmount = initialAmount.value.add(java.math.BigDecimal("3224.50"))
                assertEquals(expectedAmount, updatedBooklet!!.amount.value)
            }
        }
    }

    @Test
    @DisplayName("Should not change booklet amount when import fails")
    fun `importTransactionsFromCsv should not change booklet amount when import fails`() {
        launchWithConnectedUserInstance {
            val initialAmount = booklet.amount
            val csvContent = """
                date,label,depense,recette,tag
                invalid-date,Test,45.50,,
            """.trimIndent()

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isFailure())

            // Vérifier que le montant du livret n'a pas changé
            val accountState = FakeFactory.accountState()
            val updatedBooklets = accountState.getStates().find { it.userId == user.id }
            assertNotNull(updatedBooklets)
            val updatedBooklet = updatedBooklets!!.booklet.find { it.id == booklet.id }
            assertNotNull(updatedBooklet)
            assertEquals(initialAmount.value, updatedBooklet!!.amount.value)
        }
    }

    @Test
    @DisplayName("Should persist booklet with updated amount after successful import")
    fun `importTransactionsFromCsv should persist booklet with updated amount in repository`() {
        launchWithConnectedUserInstance {
            val initialAmount = booklet.amount
            val csvContent = """
                date,label,depense,recette,tag
                15-01-2025,Purchase,100.00,,Aucune
            """.trimIndent()

            val result = csvImportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            assertTrue(result.isSuccess())

            // Vérifier que le montant a été persisté en base
            val accountState = FakeFactory.accountState()
            val persistedBooklets = accountState.getStates().find { it.userId == user.id }
            assertNotNull(persistedBooklets)
            val persistedBooklet = persistedBooklets!!.booklet.find { it.id == booklet.id }
            assertNotNull(persistedBooklet)

            val expectedAmount = initialAmount.value.subtract(java.math.BigDecimal("100.00"))
            assertEquals(expectedAmount, persistedBooklet!!.amount.value)

            // Vérifier que les transactions ont bien été importées
            result.onSuccess { importResult ->
                assertEquals(1, importResult.transactions.size)
                assertEquals("Purchase", importResult.transactions.first().label)
            }
        }
    }
}

