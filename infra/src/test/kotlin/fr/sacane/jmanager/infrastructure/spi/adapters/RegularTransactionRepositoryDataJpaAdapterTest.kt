package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.regular.RegularTransactionRepositoryDataJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.JpaRegularTransactionTrackerRepository
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.RegularTransactionTrackerEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.RegularTransactionResourceJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class RegularTransactionRepositoryDataJpaAdapterTest(
    @Autowired private val regularTransactionAdapter: RegularTransactionRepositoryDataJpaAdapter,
    @Autowired private val regularTransactionRepository: RegularTransactionResourceJpaRepository,
    @Autowired private val trackerRepository: JpaRegularTransactionTrackerRepository,
    @Autowired private val bookletStateTestAdapter: BookletStateTestAdapter,
    @Autowired private val tagRepository: TagRepository
) : AuthenticatedUserTest() {

    private lateinit var booklet: Booklet
    private lateinit var defaultTag: Tag

    @BeforeEach
    fun setupBooklet() {
        val newBooklet = Booklet(
            label = "Test Booklet",
            amount = fr.sacane.jmanager.domain.models.Amount(1000L),
            owner = user,
        )
        bookletStateTestAdapter.init(listOf(newBooklet))
        booklet = bookletStateTestAdapter.get().find { it.label == "Test Booklet" }!!
        defaultTag = tagRepository.defaultTag()!!
    }

    @AfterEach
    fun cleanUp() {
        trackerRepository.deleteAll()
        regularTransactionRepository.deleteAll()
        bookletStateTestAdapter.clear()
    }

    @Nested
    inner class SaveRegularTransactionTest {

        @Test
        fun `should save a regular transaction successfully`() {
            val regularTransaction = RegularTransaction(
                label = "Monthly Salary",
                amount = fr.sacane.jmanager.domain.models.Amount(2500L),
                isIncome = true,
                tag = defaultTag,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val result = regularTransactionAdapter.saveRegularTransaction(
                user!!.id,
                regularTransaction,
                listOf(booklet.id!!)
            )

            assertNotNull(result)
            assertEquals("Monthly Salary", result.label)
            assertEquals(BigDecimal("2500.00"), result.amount.value)
            assertTrue(result.isIncome)
            assertNotNull(result.id)
        }

        @Test
        fun `should save a regular transaction with UntilDate frequency`() {
            val endDate = LocalDate.of(2024, 12, 31)
            val regularTransaction = RegularTransaction(
                label = "Temporary Loan",
                amount = Amount(200L),
                tag = defaultTag,
                isIncome = false,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.UntilDate(endDate),
                recurrenceRule = RecurrenceRule.Monthly(15)
            )

            val result = regularTransactionAdapter.saveRegularTransaction(
                user!!.id,
                regularTransaction,
                listOf(booklet.id!!)
            )

            assertNotNull(result)
            assertTrue(result.frequencyProperty is FrequencyProperty.UntilDate)
            assertEquals(endDate, (result.frequencyProperty as FrequencyProperty.UntilDate).date)
        }

        @Test
        fun `should save a regular transaction with SpecificRepetitionTimes frequency`() {
            val regularTransaction = RegularTransaction(
                label = "Yoga Classes - 10 sessions",
                tag = defaultTag,
                amount = fr.sacane.jmanager.domain.models.Amount(50L),
                isIncome = false,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(10),
                recurrenceRule = RecurrenceRule.Monthly(10)
            )

            val result = regularTransactionAdapter.saveRegularTransaction(
                user!!.id,
                regularTransaction,
                listOf(booklet.id!!)
            )

            assertNotNull(result)
            assertTrue(result.frequencyProperty is FrequencyProperty.SpecificRepetitionTimes)
            assertEquals(10, (result.frequencyProperty as FrequencyProperty.SpecificRepetitionTimes).number)
        }

        @Test
        fun `should throw exception when user not found`() {
            val regularTransaction = RegularTransaction(
                tag = defaultTag,
                label = "Test",
                amount = fr.sacane.jmanager.domain.models.Amount(100L),
                isIncome = true,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.now(),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )

            assertThrows(Exception::class.java) {
                regularTransactionAdapter.saveRegularTransaction(
                    UserId(UUID.randomUUID()),
                    regularTransaction,
                    listOf(booklet.id!!)
                )
            }
        }
    }

    @Nested
    inner class GetRegularTransactionByIdTest {

        @Test
        fun `should retrieve regular transaction by id`() {
            val regularTransaction = RegularTransaction(
                label = "Spotify Subscription",
                amount = Amount(999L),
                isIncome = false,
                tag = defaultTag,
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1),
                id = RegularTransactionId(UUID.randomUUID().toString())
            )

            val saved = regularTransactionAdapter.saveRegularTransaction(
                user!!.id,
                regularTransaction,
                listOf(booklet.id!!)
            )

            val result = regularTransactionAdapter.getRegularTransactionById(
                user!!.id,
                saved.id
            )

            assertNotNull(result)
            assertEquals("Spotify Subscription", result!!.label)
            assertEquals(BigDecimal("999.00"), result.amount.value)
        }

        @Test
        fun `should return null when transaction id does not exist`() {
            val result = regularTransactionAdapter.getRegularTransactionById(
                user!!.id,
                RegularTransactionId(UUID.randomUUID().toString())
            )

            assertNull(result)
        }
    }

    @Nested
    inner class GetAllRegularTransactionsTest {

        @Test
        fun `should retrieve all regular transactions for user`() {
            val transaction1 = RegularTransaction(
                label = "Salary",
                amount = fr.sacane.jmanager.domain.models.Amount(2500L),
                tag = defaultTag,
                isIncome = true,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val transaction2 = RegularTransaction(
                label = "Rent",
                amount = fr.sacane.jmanager.domain.models.Amount(800L),
                tag = defaultTag,
                isIncome = false,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.of(2024, 1, 5),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(5)
            )

            regularTransactionAdapter.saveRegularTransaction(user!!.id, transaction1, listOf(booklet.id!!))
            regularTransactionAdapter.saveRegularTransaction(user!!.id, transaction2, listOf(booklet.id!!))

            val result = regularTransactionAdapter.getAllRegularTransactions(user!!.id)

            assertEquals(2, result.size)
            assertTrue(result.any { it.label == "Salary" })
            assertTrue(result.any { it.label == "Rent" })
        }

        @Test
        fun `should return empty list when user has no regular transactions`() {
            val result = regularTransactionAdapter.getAllRegularTransactions(user!!.id)

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class GetAllRegularUsedByBookletTest {

        @Test
        fun `should retrieve regular transactions used by specific booklet`() {
            val secondBooklet = Booklet(
                label = "Second Booklet",
                amount = Amount(500L),
                owner = user,
            )
            bookletStateTestAdapter.init(listOf(secondBooklet))
            val savedSecondBooklet = bookletStateTestAdapter.get().find { it.label == "Second Booklet" }!!

            val transaction1 = RegularTransaction(
                tag = defaultTag,
                label = "Transaction for first booklet",
                amount = Amount(100L),
                isIncome = true,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.now(),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val transaction2 = RegularTransaction(
                tag = defaultTag,
                label = "Transaction for second booklet",
                amount = Amount(200L),
                isIncome = false,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.now(),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )

            regularTransactionAdapter.saveRegularTransaction(user!!.id, transaction1, listOf(booklet.id!!))
            regularTransactionAdapter.saveRegularTransaction(user!!.id, transaction2, listOf(savedSecondBooklet.id!!))

            val result = regularTransactionAdapter.getAllRegularUsedByBooklet(user!!.id, booklet.id!!)

            assertNotNull(result)
            assertEquals(1, result!!.size)
            assertEquals("Transaction for first booklet", result[0].label)
        }

        @Test
        fun `should return empty list when booklet has no regular transactions`() {
            val result = regularTransactionAdapter.getAllRegularUsedByBooklet(user!!.id, booklet.id!!)

            assertNotNull(result)
            assertTrue(result!!.isEmpty())
        }
    }

    @Nested
    inner class UpdateRegularTransactionTest {

        @Test
        fun `should update regular transaction successfully`() {
            val originalTransaction = RegularTransaction(
                label = "Gym Subscription",
                amount = fr.sacane.jmanager.domain.models.Amount(40L),
                isIncome = false,
                tag = defaultTag,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val saved = regularTransactionAdapter.saveRegularTransaction(
                user!!.id,
                originalTransaction,
                listOf(booklet.id!!)
            )

            val updatedTransaction = saved.copy(
                amount = fr.sacane.jmanager.domain.models.Amount(45L),
                label = "Gym Premium Subscription"
            )

            val result = regularTransactionAdapter.updateRegularTransaction(
                user!!.id,
                updatedTransaction,
                listOf(booklet.id!!)
            )

            assertNotNull(result)
            assertEquals("Gym Premium Subscription", result!!.label)
            assertEquals(BigDecimal("45.00"), result.amount.value)
            assertEquals(saved.id, result.id)
        }

        @Test
        fun `should return null when updating non-existing transaction`() {

            val transaction = RegularTransaction(
                label = "Non-existing",
                amount = Amount(100L),
                isIncome = true,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.now(),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1),
                tag = defaultTag,
            )

            val result = regularTransactionAdapter.updateRegularTransaction(user!!.id, transaction, listOf(booklet.id!!))

            assertNull(result)
        }

        @Test
        fun `should return null when updating transaction of another user`() {
            val transaction = RegularTransaction(
                label = "Other User Transaction",
                tag = defaultTag,
                amount = fr.sacane.jmanager.domain.models.Amount(100L),
                isIncome = true,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.now(),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val saved = regularTransactionAdapter.saveRegularTransaction(
                user!!.id,
                transaction,
                listOf(booklet.id!!)
            )

            val updatedTransaction = saved.copy(label = "Updated")
            val result = regularTransactionAdapter.updateRegularTransaction(
                UserId(UUID.randomUUID()),
                updatedTransaction,
                listOf(booklet.id!!)
            )

            assertNull(result)
        }
    }

    @Nested
    inner class DeleteRegularTransactionTest {

        @Test
        fun `should delete regular transaction successfully`() {
            val transaction = RegularTransaction(
                label = "Subscription to delete",
                amount = fr.sacane.jmanager.domain.models.Amount(1999L),
                isIncome = false,
                tag = defaultTag,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val saved = regularTransactionAdapter.saveRegularTransaction(
                user!!.id,
                transaction,
                listOf(booklet.id!!)
            )

            val result = regularTransactionAdapter.deleteRegularTransaction(user!!.id, saved.id)

            assertTrue(result)

            val getResult = regularTransactionAdapter.getRegularTransactionById(user!!.id, saved.id)
            assertNull(getResult)
        }

        @Test
        fun `should return false when deleting non-existing transaction`() {
            val result = regularTransactionAdapter.deleteRegularTransaction(
                user!!.id,
                RegularTransactionId(UUID.randomUUID().toString())
            )

            assertFalse(result)
        }

        @Test
        fun `should return false when deleting transaction of another user`() {
            val transaction = RegularTransaction(
                label = "Other User Transaction",
                amount = Amount(100L),
                isIncome = true,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.now(),
                tag = defaultTag,
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val saved = regularTransactionAdapter.saveRegularTransaction(
                user!!.id,
                transaction,
                listOf(booklet.id!!)
            )

            val result = regularTransactionAdapter.deleteRegularTransaction(
                UserId(UUID.randomUUID()),
                saved.id
            )

            assertFalse(result)

            val getResult = regularTransactionAdapter.getRegularTransactionById(user!!.id, saved.id)
            assertNotNull(getResult)
        }

        @Test
        fun `should update associated booklets when editing regular transaction`() {
            val secondBooklet = Booklet(
                label = "Second Booklet",
                amount = Amount(500L),
                owner = user,
            )
            bookletStateTestAdapter.init(listOf(secondBooklet))
            val savedSecondBooklet = bookletStateTestAdapter.get().find { it.label == "Second Booklet" }!!

            val originalTransaction = RegularTransaction(
                label = "Booklet Switch",
                amount = Amount(40L),
                isIncome = false,
                tag = defaultTag,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val saved = regularTransactionAdapter.saveRegularTransaction(
                user!!.id,
                originalTransaction,
                listOf(booklet.id!!)
            )

            val result = regularTransactionAdapter.updateRegularTransaction(
                user!!.id,
                saved.copy(label = "Booklet Switch Updated"),
                listOf(savedSecondBooklet.id!!)
            )

            assertNotNull(result)

            val firstBookletRegulars = regularTransactionAdapter.getAllRegularUsedByBooklet(user!!.id, booklet.id!!)
            val secondBookletRegulars = regularTransactionAdapter.getAllRegularUsedByBooklet(user!!.id, savedSecondBooklet.id!!)

            assertTrue(firstBookletRegulars?.none { it.id == saved.id } ?: true)
            assertTrue(secondBookletRegulars?.any { it.id == saved.id } == true)
        }

        @Test
        fun `should delete trackers linked to deleted regular transaction`() {
            val transaction = RegularTransaction(
                label = "Subscription with tracker",
                amount = Amount(1599L),
                isIncome = false,
                tag = defaultTag,
                id = RegularTransactionId(UUID.randomUUID().toString()),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val saved = regularTransactionAdapter.saveRegularTransaction(
                user!!.id,
                transaction,
                listOf(booklet.id!!)
            )

            trackerRepository.save(
                RegularTransactionTrackerEntity(
                    regularTransactionId = saved.id.value,
                    bookletId = booklet.id!!,
                    lastGeneratedDate = LocalDate.of(2024, 2, 1),
                    numberOfGeneratedTransaction = 2
                )
            )

            val trackersBeforeDelete = trackerRepository.findAllByBookletId(booklet.id!!)
            assertEquals(1, trackersBeforeDelete.size)

            val result = regularTransactionAdapter.deleteRegularTransaction(user!!.id, saved.id)
            assertTrue(result)

            val trackersAfterDelete = trackerRepository.findAllByBookletId(booklet.id!!)
            assertTrue(trackersAfterDelete.none { it.regularTransactionId == saved.id.value })
        }
    }
}

