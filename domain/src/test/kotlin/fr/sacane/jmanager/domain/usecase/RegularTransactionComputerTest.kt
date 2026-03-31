package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserAccount
import fr.sacane.jmanager.domain.fake.IdUserAccountByTransaction
import fr.sacane.jmanager.domain.fake.InMemoryTransactionRepository
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.FeatureTest
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class RegularTransactionComputerTest : FeatureTest() {

    private lateinit var transactionRepository: InMemoryTransactionRepository
    private lateinit var trackerRepository: RegularTransactionTrackerRepository
    private lateinit var regularTransactionGenerator: RegularTransactionGenerator
    private val transactionState: State<IdUserAccountByTransaction> = FakeFactory.fakeTransactionRepository()

    @BeforeEach
    fun setup() {
        transactionRepository = FakeFactory.transactionRepository()
        trackerRepository = FakeFactory.trackerRepository()
        regularTransactionGenerator = FakeFactory.regularTransactionGenerator()
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

        @Test
        fun `should not generate previsional transaction when a real transaction already exists with same regularTransactionId`() {
            launchWithConnectedUserInstance {
                val regularTransactionId = RegularTransactionId("${user.id.value}-salary")

                val monthlyTransaction = RegularTransaction(
                    label = "Salaire",
                    amount = 3000.toAmount(),
                    isIncome = true,
                    id = regularTransactionId,
                    startDate = LocalDate.of(2024, 1, 15),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(15)
                )

                // Create a real (confirmed) transaction with the same regularTransactionId
                val confirmedTransaction = Transaction(
                    id = null,
                    label = "Salaire",
                    amount = 3000.toAmount(),
                    date = LocalDate.of(2024, 3, 15),
                    isIncome = true,
                    isPreview = false,
                    regularTransactionId = regularTransactionId
                )

                // Save the confirmed transaction in the repository
                transactionState.init(
                    listOf(
                        IdUserAccountByTransaction(
                            id = IdUserAccount(user.id, booklet.id!!),
                            transactions = mutableListOf(confirmedTransaction)
                        )
                    )
                )

                // Try to generate previsional transactions for March 2024
                val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.MARCH,
                    targetYear = 2024
                )

                // Should not generate any transaction because a real one already exists
                assertEquals(0, generatedTransactions.size)
            }
        }

        @Test
        fun `should generate previsional transaction when no real transaction exists for this regularTransactionId`() {
            launchWithConnectedUserInstance {
                val regularTransactionId = RegularTransactionId("${user.id.value}-salary")

                val monthlyTransaction = RegularTransaction(
                    label = "Salaire",
                    amount = 3000.toAmount(),
                    isIncome = true,
                    id = regularTransactionId,
                    startDate = LocalDate.of(2024, 1, 15),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(15)
                )

                // Try to generate previsional transactions for March 2024
                val generatedTransactions = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.MARCH,
                    targetYear = 2024
                )

                // Should generate exactly one transaction
                assertEquals(1, generatedTransactions.size)

                val transaction = generatedTransactions[0]
                assertTrue(transaction.isPreview)
                assertEquals(regularTransactionId, transaction.regularTransactionId)
                assertEquals(LocalDate.of(2024, 3, 15), transaction.date)
            }
        }

        @Test
        fun `should not regenerate transaction for a month where it was deleted after confirmation`() {
            launchWithConnectedUserInstance {
                val regularTransactionId = RegularTransactionId("${user.id.value}-salary")

                val monthlyTransaction = RegularTransaction(
                    label = "Salaire",
                    amount = 3000.toAmount(),
                    isIncome = true,
                    id = regularTransactionId,
                    startDate = LocalDate.of(2026, 1, 15),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(15)
                )

                // 1. Generate previsional transaction for January 2026
                val firstGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.JANUARY,
                    targetYear = 2026
                )

                assertEquals(1, firstGeneration.size)
                assertTrue(firstGeneration[0].isPreview)

                // 2. Simulate confirmation: user confirms the transaction (it becomes real)
                val confirmedTransaction = firstGeneration[0].copy(isPreview = false)
                transactionRepository.save(booklet.id!!, confirmedTransaction)

                // 3. Simulate deletion: user deletes the confirmed transaction
                // This should mark January 2026 as excluded for this regular transaction
                trackerRepository.markMonthAsExcluded(
                    regularTransactionId = regularTransactionId,
                    bookletId = booklet.id!!,
                    year = 2026,
                    month = Month.JANUARY
                )

                // 4. Try to generate again for January 2026
                val secondGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.JANUARY,
                    targetYear = 2026
                )

                // Should NOT generate because this month was excluded
                assertEquals(0, secondGeneration.size)

                // 5. Generate for February 2026 - should still work
                val februaryGeneration = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.FEBRUARY,
                    targetYear = 2026
                )

                assertEquals(1, februaryGeneration.size)
                assertEquals(Month.FEBRUARY, februaryGeneration[0].date.month)
            }
        }

        @Test
        fun `virtual transactions should also respect excluded months`() {
            launchWithConnectedUserInstance {
                val regularTransactionId = RegularTransactionId("${user.id.value}-rent")

                val monthlyTransaction = RegularTransaction(
                    label = "Loyer",
                    amount = 800.toAmount(),
                    isIncome = false,
                    id = regularTransactionId,
                    startDate = LocalDate.of(2026, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                // Mark February 2026 as excluded
                trackerRepository.markMonthAsExcluded(
                    regularTransactionId = regularTransactionId,
                    bookletId = booklet.id!!,
                    year = 2026,
                    month = Month.FEBRUARY
                )

                // Calculate virtual transactions from January to April 2026
                val virtualTransactions = regularTransactionGenerator.calculateVirtualTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    startMonth = Month.JANUARY,
                    startYear = 2026,
                    endMonth = Month.APRIL,
                    endYear = 2026
                )

                // Should generate for January, March, and April (not February)
                assertEquals(3, virtualTransactions.size)

                val months = virtualTransactions.map { it.date.month }.toSet()
                assertTrue(months.contains(Month.JANUARY))
                assertFalse(months.contains(Month.FEBRUARY), "February should be excluded")
                assertTrue(months.contains(Month.MARCH))
                assertTrue(months.contains(Month.APRIL))
            }
        }

        @Test
        fun `virtual transactions should exclude already materialized physical occurrences`() {
            launchWithConnectedUserInstance {
                val regularTransactionId = RegularTransactionId("${user.id.value}-gym")

                val monthlyTransaction = RegularTransaction(
                    label = "Salle de sport",
                    amount = 35.toAmount(),
                    isIncome = false,
                    id = regularTransactionId,
                    startDate = LocalDate.of(2026, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                val existingPhysicalTransaction = Transaction(
                    id = null,
                    label = "Salle de sport",
                    amount = 35.toAmount(),
                    date = LocalDate.of(2026, 2, 1),
                    isIncome = false,
                    isPreview = false,
                    regularTransactionId = regularTransactionId
                )

                val virtualTransactions = regularTransactionGenerator.calculateVirtualTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    startMonth = Month.JANUARY,
                    startYear = 2026,
                    endMonth = Month.MARCH,
                    endYear = 2026,
                    existingPhysicalTransactions = listOf(existingPhysicalTransaction)
                )

                assertEquals(2, virtualTransactions.size)
                val generatedMonths = virtualTransactions.map { it.date.month }.toSet()
                assertTrue(generatedMonths.contains(Month.JANUARY))
                assertFalse(generatedMonths.contains(Month.FEBRUARY))
                assertTrue(generatedMonths.contains(Month.MARCH))
            }
        }

        @Test
        fun `day anchor must be preserved after a short month in non-leap year`() {
            // Monthly(29) starting Jan 29 2025: Feb clamps to 28, but Mar must be Mar 29 (not 28)
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Ancrage jour",
                    amount = 100.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id.value}-anchor-non-leap"),
                    startDate = LocalDate.of(2025, 1, 29),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(29)
                )

                val marchTransaction = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.MARCH,
                    targetYear = 2025
                )

                assertEquals(1, marchTransaction.size)
                assertEquals(LocalDate.of(2025, 3, 29), marchTransaction[0].date)
            }
        }

        @Test
        fun `day anchor must be preserved after a short month for day 31`() {
            // Monthly(31) starting Jan 31: Feb clamps to 28, Mar must be Mar 31 (not 28)
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Ancrage jour 31",
                    amount = 50.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-anchor-31"),
                    startDate = LocalDate.of(2025, 1, 31),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(31)
                )

                val marchTransaction = regularTransactionGenerator.generateMissingPrevisionalTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    targetMonth = Month.MARCH,
                    targetYear = 2025
                )

                assertEquals(1, marchTransaction.size)
                assertEquals(LocalDate.of(2025, 3, 31), marchTransaction[0].date)
            }
        }

        @Test
        fun `virtual transactions use correct anchor date after crossing a short month`() {
            // Monthly(28) starting Feb 28 2026: virtual for custom range Mar (28/02->27/03) = 28/02, Apr (28/03->27/04) = 28/03
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Aqua",
                    amount = 30.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id.value}-aqua"),
                    startDate = LocalDate.of(2026, 2, 28),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(28)
                )

                // Custom range for "March" with startDay=28: 2026-02-28 → 2026-03-27
                val marchRangeTransactions = regularTransactionGenerator.calculateVirtualTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    startMonth = Month.FEBRUARY,
                    startYear = 2026,
                    endMonth = Month.MARCH,
                    endYear = 2026
                ).filter { !it.date.isBefore(LocalDate.of(2026, 2, 28)) && !it.date.isAfter(LocalDate.of(2026, 3, 27)) }

                assertEquals(1, marchRangeTransactions.size)
                assertEquals(LocalDate.of(2026, 2, 28), marchRangeTransactions[0].date)

                // Custom range for "April" with startDay=28: 2026-03-28 → 2026-04-27
                val aprilRangeTransactions = regularTransactionGenerator.calculateVirtualTransactions(
                    bookletId = booklet.id!!,
                    regularTransactions = listOf(monthlyTransaction),
                    startMonth = Month.MARCH,
                    startYear = 2026,
                    endMonth = Month.APRIL,
                    endYear = 2026
                ).filter { !it.date.isBefore(LocalDate.of(2026, 3, 28)) && !it.date.isAfter(LocalDate.of(2026, 4, 27)) }

                assertEquals(1, aprilRangeTransactions.size)
                assertEquals(LocalDate.of(2026, 3, 28), aprilRangeTransactions[0].date)
            }
        }
    }
}
