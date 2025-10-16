package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserAccountByTransaction
import fr.sacane.jmanager.domain.fake.InMemoryRegularTrackerRepository
import fr.sacane.jmanager.domain.fake.InMemoryTransactionRepository
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyRepeatProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
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
                val monthlyTransaction = MonthlyTransaction(
                    label = "Salaire",
                    amount = 3000.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-salary"),
                    startDate = LocalDate.of(2024, 1, 15),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(15)
                )

                val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.DECEMBER,
                    targetYear = 2024
                )

                assertEquals(12, generatedTransactions.size)

                generatedTransactions.forEachIndexed { index, transaction ->
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
                val transaction1 = MonthlyTransaction(
                    label = "Loyer",
                    amount = 800.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-rent"),
                    startDate = LocalDate.of(2024, 1, 5),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(5)
                )

                val transaction2 = MonthlyTransaction(
                    label = "Facture internet",
                    amount = 50.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-internet"),
                    startDate = LocalDate.of(2024, 1, 10),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(10)
                )

                val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(transaction1, transaction2),
                    targetMonth = Month.JUNE,
                    targetYear = 2024
                )

                assertEquals(12, generatedTransactions.size)

                val rentTransactions = generatedTransactions.filter { it.label == "Loyer" }
                val internetTransactions = generatedTransactions.filter { it.label == "Facture internet" }

                assertEquals(6, rentTransactions.size)
                assertEquals(6, internetTransactions.size)

                rentTransactions.forEach {
                    assertEquals(5, it.date.dayOfMonth)
                }

                internetTransactions.forEach {
                    assertEquals(10, it.date.dayOfMonth)
                }
            }
        }

        @Test
        fun `should not generate duplicate transactions for already generated months`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = MonthlyTransaction(
                    label = "Abonnement",
                    amount = 20.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-subscription"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
                )

                val firstGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.MARCH,
                    targetYear = 2024
                )

                assertEquals(3, firstGeneration.size)

                val secondGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.JUNE,
                    targetYear = 2024
                )

                assertEquals(3, secondGeneration.size)

                val allMonths = secondGeneration.map { it.date.month }
                assertTrue(allMonths.contains(Month.APRIL))
                assertTrue(allMonths.contains(Month.MAY))
                assertTrue(allMonths.contains(Month.JUNE))
            }
        }
    }

    @Nested
    inner class GenerateMissingPrevisionalTransactionsWithUntilDate {

        @Test
        fun `should generate transactions only until the specified end date`() {
            launchWithConnectedUserInstance {
                val endDate = LocalDate.of(2024, 6, 30)
                val monthlyTransaction = MonthlyTransaction(
                    label = "Prêt temporaire",
                    amount = 500.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-loan"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.UntilDate(endDate),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
                )

                val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.DECEMBER,
                    targetYear = 2024
                )

                assertEquals(6, generatedTransactions.size)

                generatedTransactions.forEach { transaction ->
                    assertTrue(transaction.date.isBefore(endDate) || transaction.date.isEqual(endDate))
                }

                val lastTransaction = generatedTransactions.maxByOrNull { it.date }
                assertNotNull(lastTransaction)
                assertEquals(Month.JUNE, lastTransaction?.date?.month)
            }
        }

        @Test
        fun `should not generate any transaction when start date is after end date`() {
            launchWithConnectedUserInstance {
                val endDate = LocalDate.of(2024, 3, 31)
                val monthlyTransaction = MonthlyTransaction(
                    label = "Prêt expiré",
                    amount = 200.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-expired-loan"),
                    startDate = LocalDate.of(2024, 5, 1),
                    frequencyProperty = FrequencyProperty.UntilDate(endDate),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
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
                val monthlyTransaction = MonthlyTransaction(
                    label = "Formation - 5 mois",
                    amount = 100.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-training"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(5),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
                )

                val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.DECEMBER,
                    targetYear = 2024
                )

                assertEquals(5, generatedTransactions.size)

                val months = generatedTransactions.map { it.date.month }
                assertEquals(listOf(Month.JANUARY, Month.FEBRUARY, Month.MARCH, Month.APRIL, Month.MAY), months)
            }
        }

        @Test
        fun `should respect max repetition count across multiple generations`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = MonthlyTransaction(
                    label = "Cours - 3 séances",
                    amount = 50.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-course"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(3),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
                )

                val firstGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.FEBRUARY,
                    targetYear = 2024
                )

                assertEquals(2, firstGeneration.size)

                val secondGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.JUNE,
                    targetYear = 2024
                )

                assertEquals(1, secondGeneration.size)
            }
        }
    }

    @Nested
    inner class TrackerManagement {

        @Test
        fun `should create tracker after first generation`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = MonthlyTransaction(
                    label = "Test tracker",
                    amount = 100.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-tracker-test"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
                )

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
                assertEquals(3, tracker?.numberOfGeneratedTransaction)
                assertEquals(LocalDate.of(2024, 3, 31), tracker?.lastGeneratedDate)
            }
        }

        @Test
        fun `should update tracker on subsequent generations`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = MonthlyTransaction(
                    label = "Test update tracker",
                    amount = 100.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-update-tracker"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
                )

                regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.MARCH,
                    targetYear = 2024
                )

                val trackerAfterFirst = trackerRepository.findTracker(monthlyTransaction.id, booklet.id)
                assertEquals(3, trackerAfterFirst?.numberOfGeneratedTransaction)

                regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.JUNE,
                    targetYear = 2024
                )

                val trackerAfterSecond = trackerRepository.findTracker(monthlyTransaction.id!!, booklet.id!!)
                assertEquals(6, trackerAfterSecond?.numberOfGeneratedTransaction)
                assertEquals(LocalDate.of(2024, 6, 30), trackerAfterSecond?.lastGeneratedDate)
            }
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `should handle leap year correctly for February`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = MonthlyTransaction(
                    label = "Test leap year",
                    amount = 100.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-leap"),
                    startDate = LocalDate.of(2024, 1, 29),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(29)
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
                val monthlyTransaction = MonthlyTransaction(
                    label = "Test day 31",
                    amount = 100.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-day31"),
                    startDate = LocalDate.of(2024, 1, 31),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(31)
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
                val monthlyTransaction = MonthlyTransaction(
                    label = "Future transaction",
                    amount = 100.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-future"),
                    startDate = LocalDate.of(2024, 6, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
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