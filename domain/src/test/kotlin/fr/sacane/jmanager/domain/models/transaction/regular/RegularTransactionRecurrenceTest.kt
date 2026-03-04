package fr.sacane.jmanager.domain.models.transaction.regular

import fr.sacane.jmanager.domain.fake.InMemoryRegularTrackerRepository
import fr.sacane.jmanager.domain.fake.InMemoryTransactionRepository
import fr.sacane.jmanager.domain.port.FeatureTest
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.usecase.RegularTransactionGeneratorService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

class RegularTransactionRecurrenceTest : FeatureTest() {

    @Nested
    inner class ValidationTests {
        @Test
        fun `yearly with invalid month should throw`() {
            assertThrows(IllegalArgumentException::class.java) {
                RecurrenceRule.Yearly(0, 10)
            }
        }

        @Test
        fun `yearly with invalid day should throw`() {
            assertThrows(IllegalArgumentException::class.java) {
                RecurrenceRule.Yearly(1, 0)
            }
        }

        @Test
        fun `weekly with empty days should throw`() {
            assertThrows(IllegalArgumentException::class.java) {
                RecurrenceRule.Weekly(emptySet())
            }
        }
    }

    @Nested
    inner class GenerationTests {

        @Test
        fun `should generate yearly transaction on specified month and day`() {
            val db = fr.sacane.jmanager.domain.InMemoryDatabase()
            val transactionRepository = InMemoryTransactionRepository(db)
            val trackerRepository = InMemoryRegularTrackerRepository(db)
            val generator = RegularTransactionGeneratorService(transactionRepository, trackerRepository)

            val yearly = RegularTransaction(
                label = "Assurance",
                amount = 120.toAmount(),
                isIncome = false,
                id = RegularTransactionId("yearly-1"),
                startDate = LocalDate.of(2023, 6, 15),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Yearly(month = 3, dayOfMonth = 31)
            )

            val generated = generator.generateMissingPrevisionalTransactions(
                bookletId = java.util.UUID.randomUUID(),
                regularTransactions = listOf(yearly),
                targetMonth = Month.MARCH,
                targetYear = 2024
            )

            assertEquals(1, generated.size)
            val tx = generated.first()
            assertEquals(31, tx.date.dayOfMonth)
            assertEquals(Month.MARCH, tx.date.month)
            assertEquals(2024, tx.date.year)
        }

        @Test
        fun `should generate weekly transactions for specified days`() {
            val db = fr.sacane.jmanager.domain.InMemoryDatabase()
            val transactionRepository = InMemoryTransactionRepository(db)
            val trackerRepository = InMemoryRegularTrackerRepository(db)
            val generator = RegularTransactionGeneratorService(transactionRepository, trackerRepository)

            val weekly = RegularTransaction(
                label = "Gym",
                amount = 10.toAmount(),
                isIncome = false,
                id = RegularTransactionId("weekly-1"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
            )

            val generated = generator.generateMissingPrevisionalTransactions(
                bookletId = java.util.UUID.randomUUID(),
                regularTransactions = listOf(weekly),
                targetMonth = Month.JANUARY,
                targetYear = 2024
            )

            assertTrue(generated.size >= 1)
            generated.forEach {
                assertTrue(it.date.dayOfWeek == DayOfWeek.MONDAY || it.date.dayOfWeek == DayOfWeek.WEDNESDAY)
                assertEquals(2024, it.date.year)
                assertEquals(Month.JANUARY, it.date.month)
            }
        }
    }
}
