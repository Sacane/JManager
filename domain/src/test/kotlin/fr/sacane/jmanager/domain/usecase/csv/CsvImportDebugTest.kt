package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.FeatureTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("CSV Import Debug Tests")
class CsvImportDebugTest : FeatureTest() {

    private val csvImportFeature = FakeFactory.csvImportFeature

    @Test
    @DisplayName("Debug: Validate CSV with day-only dates")
    fun `debug validateCsvFile with day-only dates`() {
        launchWithConnectedUserInstance {
            val csvContent = """
                date,label,depense,recette,tag
                1,Groceries,45.50,,Alimentation & Restaurant
                15,Transport,30.00,,Transport
            """.trimIndent()

            val result = csvImportFeature.validateCsvFile(
                tokenValue,
                booklet.id!!,
                csvContent,
                month = 1,
                year = 2026
            )

            println("Result status: ${result.status}")
            println("Result isSuccess: ${result.isSuccess()}")
            println("Result isFailure: ${result.isFailure()}")
            println("Result message: ${result.message}")

            if (result.isSuccess()) {
                result.onSuccess { report ->
                    println("Report hasErrors: ${report.hasErrors}")
                    println("Report canImport: ${report.canImport}")
                    println("Report totalLines: ${report.totalLines}")
                    println("Report validLines: ${report.validLines}")
                    println("Report errors: ${report.errors}")
                    println("Report warnings: ${report.warnings}")
                }
            } else {
                println("Validation failed with status: ${result.status}")
            }

            assertTrue(result.isSuccess())
        }
    }
}

