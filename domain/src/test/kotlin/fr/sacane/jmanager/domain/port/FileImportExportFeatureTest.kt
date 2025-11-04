package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DisplayName("FileImportExportFeature Tests")
class FileImportExportFeatureTest : FeatureTest() {

    private val fileImportExportFeature = FakeFactory.fileImportExportFeature

    @Test
    @DisplayName("Should fail when user is not authenticated")
    fun `validateCsvFile should fail when user is not authenticated`() {
        val invalidToken = "invalid-token"

        val result = fileImportExportFeature.validateCsvFile(
            invalidToken,
            UUID.randomUUID(),
            "csv content"
        )

        Assertions.assertTrue(result.isFailure())
        Assertions.assertEquals(ResultState.UNAUTHORIZED, result.status)
    }

    @Test
    @DisplayName("Should fail when booklet does not exist")
    fun `validateCsvFile should fail when booklet does not exist`() {
        launchWithConnectedUserInstance {
            val nonExistentBookletId = UUID.randomUUID()

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                nonExistentBookletId,
                "csv content"
            )

            Assertions.assertTrue(result.isFailure())
            Assertions.assertEquals(ResultState.NOT_FOUND, result.status)
        }
    }

    @Test
    @DisplayName("Should fail when user does not own the booklet")
    fun `validateCsvFile should fail when user does not own the booklet`() {
        launchWithConnectedUserInstance {
            val otherUser = createAccount(
                User(
                    UserId(UUID.randomUUID()),
                    "otherUser",
                    "other@test.fr"
                ),
                "other account",
                100.toAmount()
            )

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                otherUser.id!!,
                "csv content"
            )

            Assertions.assertTrue(result.isFailure())
            Assertions.assertEquals(ResultState.FORBIDDEN, result.status)
        }
    }

    @Test
    @DisplayName("Should return report with error when CSV is empty")
    fun `validateCsvFile should return report with error when CSV is empty`() {
        launchWithConnectedUserInstance {
            val csvContent = ""

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertTrue(report.hasErrors)
                Assertions.assertFalse(report.canImport)
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

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertTrue(report.hasErrors)
                Assertions.assertFalse(report.canImport)
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

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertTrue(report.hasErrors)
                Assertions.assertFalse(report.canImport)
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

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertFalse(report.hasErrors)
                Assertions.assertTrue(report.canImport)
                Assertions.assertEquals(2, report.totalLines)
                Assertions.assertEquals(2, report.validLines)
                Assertions.assertEquals(0, report.errors.size)
                Assertions.assertEquals(0, report.warnings.size)
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

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertFalse(report.hasErrors)
                Assertions.assertTrue(report.canImport)
                Assertions.assertEquals(1, report.totalLines)
                Assertions.assertEquals(1, report.validLines)
                Assertions.assertEquals(0, report.errors.size)
                Assertions.assertEquals(1, report.warnings.size)
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

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertTrue(report.hasErrors)
                Assertions.assertFalse(report.canImport)
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

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertTrue(report.hasErrors)
                Assertions.assertFalse(report.canImport)
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

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertTrue(report.hasErrors)
                Assertions.assertFalse(report.canImport)
            }
        }
    }

    @Test
    @DisplayName("Should accept comma as decimal separator")
    fun `validateCsvFile should accept comma as decimal separator`() {
        launchWithConnectedUserInstance {
            val csvContent = "date,label,depense,recette,tag\n15-01-2025,Test,\"45,50\",,\n"

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertFalse(report.hasErrors)
                Assertions.assertTrue(report.canImport)
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

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertTrue(report.hasErrors)
                Assertions.assertFalse(report.canImport)
            }
        }
    }

    @Test
    fun `importTransactionsFromCsv should fail when user is not authenticated`() {
        val invalidToken = "invalid-token"

        val result = fileImportExportFeature.importTransactionsFromCsv(
            invalidToken,
            UUID.randomUUID(),
            "csv content"
        )

        Assertions.assertTrue(result.isFailure())
        Assertions.assertEquals(ResultState.UNAUTHORIZED, result.status)
    }

    @Test
    fun `importTransactionsFromCsv should fail when booklet does not exist`() {
        launchWithConnectedUserInstance {
            val nonExistentBookletId = UUID.randomUUID()

            val result = fileImportExportFeature.importTransactionsFromCsv(
                tokenValue,
                nonExistentBookletId,
                "csv content"
            )

            Assertions.assertTrue(result.isFailure())
            Assertions.assertEquals(ResultState.NOT_FOUND, result.status)
        }
    }

    @Test
    fun `importTransactionsFromCsv should fail when user does not own the booklet`() {
        launchWithConnectedUserInstance {
            val otherUser = createAccount(
                User(
                    UserId(UUID.randomUUID()),
                    "otherUser",
                    "other@test.fr"
                ),
                "other account",
                100.toAmount()
            )

            val result = fileImportExportFeature.importTransactionsFromCsv(
                tokenValue,
                otherUser.id!!,
                "csv content"
            )

            Assertions.assertTrue(result.isFailure())
            Assertions.assertEquals(ResultState.FORBIDDEN, result.status)
        }
    }

    @Test
    fun `importTransactionsFromCsv should fail when CSV is empty`() {
        launchWithConnectedUserInstance {
            val result = fileImportExportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                ""
            )

            Assertions.assertTrue(result.isFailure())
            Assertions.assertEquals(ResultState.INVALID, result.status)
            Assertions.assertTrue(result.message.contains("empty"))
        }
    }

    @Test
    fun `importTransactionsFromCsv should fail when CSV header is invalid`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,wrong_column,recette,tag
                15-01-2025,Test,10.00,,
            """.trimIndent()

            val result = fileImportExportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isFailure())
            Assertions.assertEquals(ResultState.INVALID, result.status)
            Assertions.assertTrue(result.message.contains("header"))
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

            val result = fileImportExportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                Assertions.assertEquals(2, importResult.successCount)
                Assertions.assertEquals(0, importResult.failedLines.size)

                val accountState = FakeFactory.accountState()
                val updatedBooklets = accountState.getStates().find { it.userId == user.id }
                Assertions.assertNotNull(updatedBooklets)
                val updatedBooklet = updatedBooklets!!.booklet.find { it.id == booklet.id }
                Assertions.assertNotNull(updatedBooklet)

                val expectedAmount = initialAmount.value.subtract(BigDecimal("75.50"))
                Assertions.assertEquals(expectedAmount, updatedBooklet!!.amount.value)
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

            val result = fileImportExportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                Assertions.assertEquals(2, importResult.successCount)
                Assertions.assertEquals(0, importResult.failedLines.size)

                val accountState = FakeFactory.accountState()
                val updatedBooklets = accountState.getStates().find { it.userId == user.id }
                Assertions.assertNotNull(updatedBooklets)
                val updatedBooklet = updatedBooklets!!.booklet.find { it.id == booklet.id }
                Assertions.assertNotNull(updatedBooklet)

                val expectedAmount = initialAmount.value.add(BigDecimal("3000.00"))
                Assertions.assertEquals(expectedAmount, updatedBooklet!!.amount.value)
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

            val result = fileImportExportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                Assertions.assertEquals(4, importResult.successCount)
                Assertions.assertEquals(0, importResult.failedLines.size)

                val accountState = FakeFactory.accountState()
                val updatedBooklets = accountState.getStates().find { it.userId == user.id }
                Assertions.assertNotNull(updatedBooklets)
                val updatedBooklet = updatedBooklets!!.booklet.find { it.id == booklet.id }
                Assertions.assertNotNull(updatedBooklet)

                val expectedAmount = initialAmount.value.add(BigDecimal("3224.50"))
                Assertions.assertEquals(expectedAmount, updatedBooklet!!.amount.value)
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

            val result = fileImportExportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isFailure())

            val accountState = FakeFactory.accountState()
            val updatedBooklets = accountState.getStates().find { it.userId == user.id }
            Assertions.assertNotNull(updatedBooklets)
            val updatedBooklet = updatedBooklets!!.booklet.find { it.id == booklet.id }
            Assertions.assertNotNull(updatedBooklet)
            Assertions.assertEquals(initialAmount.value, updatedBooklet!!.amount.value)
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

            val result = fileImportExportFeature.importTransactionsFromCsv(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())

            val accountState = FakeFactory.accountState()
            val persistedBooklets = accountState.getStates().find { it.userId == user.id }
            Assertions.assertNotNull(persistedBooklets)
            val persistedBooklet = persistedBooklets!!.booklet.find { it.id == booklet.id }
            Assertions.assertNotNull(persistedBooklet)

            val expectedAmount = initialAmount.value.subtract(BigDecimal("100.00"))
            Assertions.assertEquals(expectedAmount, persistedBooklet!!.amount.value)

            result.onSuccess { importResult ->
                Assertions.assertEquals(1, importResult.transactions.size)
                Assertions.assertEquals("Purchase", importResult.transactions.first().label)
            }
        }
    }

    @Test
    @DisplayName("Should validate CSV with day-only dates when month and year are provided")
    fun `validateCsvFile should accept day-only dates when month and year are provided`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                1,Groceries,45.50,,Alimentation & Restaurant
                15,Transport,30.00,,Transport
            """.trimIndent()

            val result = fileImportExportFeature.validateCsvFile(
                token = tokenValue,
                bookletId = booklet.id!!,
                csvContent = csvContent,
                month = 1,
                year = 2026
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertFalse(report.hasErrors)
                Assertions.assertTrue(report.canImport)
                Assertions.assertEquals(2, report.totalLines)
                Assertions.assertEquals(2, report.validLines)
            }
        }
    }

    @Test
    @DisplayName("Should fail validation when day-only date is provided without month and year")
    fun `validateCsvFile should fail when day-only date without month and year`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                1,Groceries,45.50,,Alimentation & Restaurant
            """.trimIndent()

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertTrue(report.hasErrors)
                Assertions.assertFalse(report.canImport)
            }
        }
    }

    @Test
    @DisplayName("Should import transactions with day-only dates when month and year are provided")
    fun `importTransactionsFromCsv should import with day-only dates and month year`() {
        launchWithConnectedUserInstance {
            val initialAmount = booklet.amount
            val csvContent = """
                date,label,depense,recette,tag
                1,Groceries,45.50,,Alimentation & Restaurant
                15,Salary,,2500.00,Aucune
            """.trimIndent()

            val result = fileImportExportFeature.importTransactionsFromCsv(
                token = tokenValue,
                bookletId = booklet.id!!,
                csvContent = csvContent,
                skipValidation = false,
                month = 1,
                year = 2026
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                Assertions.assertEquals(2, importResult.successCount)
                Assertions.assertEquals(0, importResult.failedLines.size)

                val transaction1 = importResult.transactions.find { it.label == "Groceries" }
                Assertions.assertNotNull(transaction1)
                Assertions.assertEquals(LocalDate.of(2026, 1, 1), transaction1!!.date)

                val transaction2 = importResult.transactions.find { it.label == "Salary" }
                Assertions.assertNotNull(transaction2)
                Assertions.assertEquals(LocalDate.of(2026, 1, 15), transaction2!!.date)

                val accountState = FakeFactory.accountState()
                val updatedBooklets = accountState.getStates().find { it.userId == user.id }
                Assertions.assertNotNull(updatedBooklets)
                val updatedBooklet = updatedBooklets!!.booklet.find { it.id == booklet.id }
                Assertions.assertNotNull(updatedBooklet)

                val expectedAmount = initialAmount.value.add(BigDecimal("2454.50"))
                Assertions.assertEquals(expectedAmount, updatedBooklet!!.amount.value)
            }
        }
    }

    @Test
    @DisplayName("Should accept mixed full dates and day-only dates")
    fun `importTransactionsFromCsv should accept mixed date formats`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                15-02-2026,Full Date Transaction,100.00,,Aucune
                20,Day Only Transaction,50.00,,Aucune
            """.trimIndent()

            val result = fileImportExportFeature.importTransactionsFromCsv(
                token = tokenValue,
                bookletId = booklet.id!!,
                csvContent = csvContent,
                skipValidation = false,
                month = 1,
                year = 2026
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { importResult ->
                Assertions.assertEquals(2, importResult.successCount)
                Assertions.assertEquals(0, importResult.failedLines.size)

                val fullDateTx = importResult.transactions.find { it.label == "Full Date Transaction" }
                Assertions.assertNotNull(fullDateTx)
                Assertions.assertEquals(LocalDate.of(2026, 2, 15), fullDateTx!!.date)

                val dayOnlyTx = importResult.transactions.find { it.label == "Day Only Transaction" }
                Assertions.assertNotNull(dayOnlyTx)
                Assertions.assertEquals(LocalDate.of(2026, 1, 20), dayOnlyTx!!.date)
            }
        }
    }

    @Test
    @DisplayName("Should fail when day-only date is invalid (e.g., 32)")
    fun `validateCsvFile should fail when day-only date is out of range`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                32,Invalid Day,45.50,,Aucune
            """.trimIndent()

            val result = fileImportExportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent,
                month = 1,
                year = 2026
            )

            Assertions.assertTrue(result.isSuccess())
            result.onSuccess { report ->
                Assertions.assertTrue(report.hasErrors)
                Assertions.assertFalse(report.canImport)
            }
        }
    }
}
