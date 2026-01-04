package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserAccountByTransaction
import fr.sacane.jmanager.domain.fake.InMemoryRegularTrackerRepository
import fr.sacane.jmanager.domain.fake.InMemoryTransactionRepository
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.FeatureTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class RegularTransactionComputerTest : FeatureTest() {

    private lateinit var transactionRepository: InMemoryTransactionRepository
    private lateinit var trackerRepository: InMemoryRegularTrackerRepository
    private lateinit var regularTransactionGenerator: RegularTransactionGenerator
    private val transactionState: State<IdUserAccountByTransaction> = FakeFactory.fakeTransactionRepository()

    @BeforeEach
    fun setup() {
        transactionRepository = InMemoryTransactionRepository(fr.sacane.jmanager.domain.InMemoryDatabase())
        trackerRepository = InMemoryRegularTrackerRepository(fr.sacane.jmanager.domain.InMemoryDatabase())
        regularTransactionGenerator = RegularTransactionGeneratorService(
            transactionRepository,
            trackerRepository
        )
    }

    @AfterEach
    fun tearDown() {
        transactionState.clear()
    }

    @Nested
    inner class GenerateMissingPrevisionalTransactionsWithForeverFrequency {

        @Test
        fun `should generate monthly transactions for a full year with Forever frequency`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Salaire",
                    amount = 3000.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-salary"),
                    startDate = LocalDate.of(2024, 1, 15),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(15)
                )

                // Generate transactions month by month (new behavior: only generates for target month)
                val allGeneratedTransactions = mutableListOf<Transaction>()
                for (month in 1..12) {
                    val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                        bookletId = booklet.id!!,
                        regularTransactions = listOf(monthlyTransaction),
                        targetMonth = Month.of(month),
                        targetYear = 2024
                    )
                    allGeneratedTransactions.addAll(generatedTransactions)
                }

                assertEquals(12, allGeneratedTransactions.size)

                allGeneratedTransactions.forEachIndexed { index, transaction ->
                    assertEquals("Salaire", transaction.label)
                    assertEquals(3000.toAmount(), transaction.amount)
                    assertTrue(transaction.isIncome)
                    assertTrue(transaction.isPreview)
                    assertEquals(15, transaction.date.dayOfMonth)
                    assertEquals(index + 1, transaction.date.monthValue)
                    assertEquals(2024, transaction.date.year)
                }
            }
        }

        @Test
        fun `should generate multiple monthly transactions for the same month`() {
            launchWithConnectedUserInstance {
                val transaction1 = RegularTransaction(
                    label = "Loyer",
                    amount = 800.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-rent"),
                    startDate = LocalDate.of(2024, 1, 5),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(5)
                )

                val transaction2 = RegularTransaction(
                    label = "Facture internet",
                    amount = 50.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-internet"),
                    startDate = LocalDate.of(2024, 1, 10),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(10)
                )

                // Only generates transactions for June
                val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(transaction1, transaction2),
                    targetMonth = Month.JUNE,
                    targetYear = 2024
                )

                // Should generate 2 transactions: 1 for rent and 1 for internet in June
                assertEquals(2, generatedTransactions.size)

                val rentTransactions = generatedTransactions.filter { it.label == "Loyer" }
                val internetTransactions = generatedTransactions.filter { it.label == "Facture internet" }

                assertEquals(1, rentTransactions.size)
                assertEquals(1, internetTransactions.size)

                rentTransactions.forEach {
                    assertEquals(5, it.date.dayOfMonth)
                    assertEquals(6, it.date.monthValue)
                }

                internetTransactions.forEach {
                    assertEquals(10, it.date.dayOfMonth)
                    assertEquals(6, it.date.monthValue)
                }
            }
        }

        @Test
        fun `should not generate duplicate transactions for already generated months`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Abonnement",
                    amount = 20.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-subscription"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                // Generate for March (only March will be generated)
                val firstGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.MARCH,
                    targetYear = 2024
                )

                assertEquals(1, firstGeneration.size)
                assertEquals(Month.MARCH, firstGeneration[0].date.month)

                // Generate for June (only June will be generated, not March again)
                val secondGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.JUNE,
                    targetYear = 2024
                )

                assertEquals(1, secondGeneration.size)
                assertEquals(Month.JUNE, secondGeneration[0].date.month)

                // Try to generate March again - should return empty because it's already generated
                val thirdGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.MARCH,
                    targetYear = 2024
                )

                assertEquals(0, thirdGeneration.size)
            }
        }
    }

    @Nested
    inner class GenerateMissingPrevisionalTransactionsWithUntilDate {

        @Test
        fun `should generate transactions only until the specified end date`() {
            launchWithConnectedUserInstance {
                val endDate = LocalDate.of(2024, 6, 30)
                val monthlyTransaction = RegularTransaction(
                    label = "Prêt temporaire",
                    amount = 500.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-loan"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.UntilDate(endDate),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                // Generate transactions month by month
                val allGeneratedTransactions = mutableListOf<Transaction>()
                for (month in 1..12) {
                    val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                        bookletId = booklet.id!!,
                        regularTransactions = listOf(monthlyTransaction),
                        targetMonth = Month.of(month),
                        targetYear = 2024
                    )
                    allGeneratedTransactions.addAll(generatedTransactions)
                }

                // Should only generate until June (6 months)
                assertEquals(6, allGeneratedTransactions.size)

                allGeneratedTransactions.forEach { transaction ->
                    assertTrue(transaction.date.isBefore(endDate) || transaction.date.isEqual(endDate))
                }

                val lastTransaction = allGeneratedTransactions.maxByOrNull { it.date }
                assertNotNull(lastTransaction)
                assertEquals(Month.JUNE, lastTransaction?.date?.month)
            }
        }

        @Test
        fun `should not generate any transaction when start date is after end date`() {
            launchWithConnectedUserInstance {
                val endDate = LocalDate.of(2024, 3, 31)
                val monthlyTransaction = RegularTransaction(
                    label = "Prêt expiré",
                    amount = 200.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-expired-loan"),
                    startDate = LocalDate.of(2024, 5, 1),
                    frequencyProperty = FrequencyProperty.UntilDate(endDate),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.DECEMBER,
                    targetYear = 2024
                )

                assertEquals(0, generatedTransactions.size)
            }
        }
    }

    @Nested
    inner class GenerateMissingPrevisionalTransactionsWithSpecificRepetition {

        @Test
        fun `should generate only the specified number of transactions`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Formation - 5 mois",
                    amount = 100.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-training"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(5),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                // Generate transactions month by month
                val allGeneratedTransactions = mutableListOf<Transaction>()
                for (month in 1..12) {
                    val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                        bookletId = booklet.id!!,
                        regularTransactions = listOf(monthlyTransaction),
                        targetMonth = Month.of(month),
                        targetYear = 2024
                    )
                    allGeneratedTransactions.addAll(generatedTransactions)
                }

                // Should only generate 5 transactions total
                assertEquals(5, allGeneratedTransactions.size)

                val months = allGeneratedTransactions.map { it.date.month }
                assertEquals(listOf(Month.JANUARY, Month.FEBRUARY, Month.MARCH, Month.APRIL, Month.MAY), months)
            }
        }

        @Test
        fun `should respect max repetition count across multiple generations`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Cours - 3 séances",
                    amount = 50.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-course"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(3),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                // Generate January
                val firstGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.JANUARY,
                    targetYear = 2024
                )

                assertEquals(1, firstGeneration.size)

                // Generate February
                val secondGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.FEBRUARY,
                    targetYear = 2024
                )

                assertEquals(1, secondGeneration.size)

                // Generate March
                val thirdGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.MARCH,
                    targetYear = 2024
                )

                assertEquals(1, thirdGeneration.size)

                // Try to generate April - should return 0 because max is 3
                val fourthGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.APRIL,
                    targetYear = 2024
                )

                assertEquals(0, fourthGeneration.size)
            }
        }
    }

    @Nested
    inner class TrackerManagement {

        @Test
        fun `should create tracker after first generation`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Test tracker",
                    amount = 100.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-tracker-test"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                // Generate only for March
                regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.MARCH,
                    targetYear = 2024
                )

                val tracker = trackerRepository.findTracker(monthlyTransaction.id!!, booklet.id!!)

                assertNotNull(tracker)
                assertEquals(monthlyTransaction.id, tracker?.regularTransactionId)
                assertEquals(booklet.id, tracker?.bookletId)
                // Only 1 transaction generated (March only)
                assertEquals(1, tracker?.numberOfGeneratedTransaction)
                assertEquals(LocalDate.of(2024, 3, 31), tracker?.lastGeneratedDate)
            }
        }

        @Test
        fun `should update tracker on subsequent generations`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Test update tracker",
                    amount = 100.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-update-tracker"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                // Generate March (only March)
                regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.MARCH,
                    targetYear = 2024
                )

                val trackerAfterFirst = trackerRepository.findTracker(monthlyTransaction.id, booklet.id)
                assertEquals(1, trackerAfterFirst?.numberOfGeneratedTransaction)
                assertEquals(LocalDate.of(2024, 3, 31), trackerAfterFirst?.lastGeneratedDate)

                // Generate June (only June)
                regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.JUNE,
                    targetYear = 2024
                )

                val trackerAfterSecond = trackerRepository.findTracker(monthlyTransaction.id!!, booklet.id!!)
                assertEquals(2, trackerAfterSecond?.numberOfGeneratedTransaction)
                assertEquals(LocalDate.of(2024, 6, 30), trackerAfterSecond?.lastGeneratedDate)
            }
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `should handle leap year correctly for February`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Test leap year",
                    amount = 100.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-leap"),
                    startDate = LocalDate.of(2024, 1, 29),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(29)
                )

                val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.FEBRUARY,
                    targetYear = 2024
                )

                val februaryTransaction = generatedTransactions.find { it.date.month == Month.FEBRUARY }
                assertNotNull(februaryTransaction)
                assertEquals(29, februaryTransaction?.date?.dayOfMonth)
            }
        }

        @Test
        fun `should handle day 31 for months with only 30 days`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Test day 31",
                    amount = 100.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-day31"),
                    startDate = LocalDate.of(2024, 1, 31),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(31)
                )

                val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.APRIL,
                    targetYear = 2024
                )

                val aprilTransaction = generatedTransactions.find { it.date.month == Month.APRIL }
                assertNotNull(aprilTransaction)
                assertTrue(aprilTransaction?.date?.dayOfMonth in 30..31)
            }
        }

        @Test
        fun `should return empty list when no regular transactions provided`() {
            launchWithConnectedUserInstance {
                val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = emptyList(),
                    targetMonth = Month.JUNE,
                    targetYear = 2024
                )

                assertEquals(0, generatedTransactions.size)
            }
        }

        @Test
        fun `should not generate transactions before start date`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Future transaction",
                    amount = 100.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-future"),
                    startDate = LocalDate.of(2024, 6, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.MARCH,
                    targetYear = 2024
                )

                assertEquals(0, generatedTransactions.size)
            }
        }
    }
}
