package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.*
import fr.sacane.jmanager.domain.fake.BookletsByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserBooklet
import fr.sacane.jmanager.domain.fake.IdBookletByTransaction
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fixture.TransactionFixture
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.input.transaction.*
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

fun <T> T.asSingleton(): List<T> = listOf(this)

class TransactionFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val transactionState: State<IdBookletByTransaction> = factory.fakeTransactionRepository()
    private val bookletState: State<BookletsByOwner> = factory.bookletState()
    private val bookTransactionUseCase: BookTransactionUseCase = factory.bookTransactionService
    private val retrieveTransactionsByMonthAndYearUseCase: RetrieveTransactionsByMonthAndYearUseCase = factory.retrieveTransactionsByMonthAndYearService
    private val editTransactionUseCase: EditTransactionUseCase = factory.editTransactionService
    private val findTransactionByIdUseCase: FindTransactionByIdUseCase = factory.findTransactionByIdService
    private val deleteTransactionsByIdsUseCase: DeleteTransactionsByIdsUseCase = factory.deleteTransactionsByIdsService
    private val confirmPreviewTransactionUseCase: ConfirmPreviewTransactionUseCase = factory.confirmPreviewTransactionService

    @AfterEach
    fun clearUp() {
        factory.clearAll()
    }

    private fun tx(label: String, amount: Long, isIncome: Boolean, date: LocalDate = LocalDate.now(), tag: Tag? = null, isPreview: Boolean = false): Transaction {
        return TransactionFixture.aTransaction(label = label, amount = Amount(amount), isIncome = isIncome, date = date, tag = tag ?: factory.fakeTagRepository().defaultTag(), isPreview = isPreview)
    }

    private fun anIntruder() = scenario.withUser(
        UserFixture.aUserWithPassword(user = UserFixture.aUser(username = "intruder", email = "intruder@jmanager.fr"))
    )

    @Nested
    inner class SaveTransactionInBookletFeatureTest {
        @Test
        fun `When I add a new transaction, it should persist it and update the booklet amount when its income and outcome`() {
            val ctx = scenario.withUser().withBooklet()
            val transactionToSave = tx("test", 100, true)
            val transactionToSave2 = tx("test", 50, false)

            bookTransactionUseCase.handle(BookTransactionCommand(ctx.userId, ctx.booklet.label, transactionToSave))
                .assertTrue {
                    this.transaction.amount == transactionToSave.amount && this.transaction.label == transactionToSave.label
                }
            bookTransactionUseCase.handle(BookTransactionCommand(ctx.userId, ctx.booklet.label, transactionToSave2))
                .assertTrue {
                    this.transaction.amount == transactionToSave2.amount && this.transaction.label == transactionToSave2.label
                }

            val bookletStates = bookletState.getStates()
            val bookletByOwnerTarget = bookletStates.find { it.userId == ctx.userId }
            val bookletExpected = bookletByOwnerTarget?.booklets?.find { it.id == ctx.booklet.id }
            assertNotNull(bookletExpected)
            assertEquals(Amount(50), bookletExpected?.amount)
        }

        @Test
        fun `When I add a transaction in a booklet that already have some, its position should be coherent regarding the date`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("test1", 100, true, "01/01/2024".toDate()),
                tx("test2", 100, true, "02/01/2024".toDate()),
                tx("tes3", 100, true, "03/01/2024".toDate()),
                tx("test4", 100, true, "04/01/2024".toDate())
            )))

            val toInsertAtFirst = tx("test0", 100, true, "31/12/2023".toDate())
            val toInsertAtLast = tx("test100", 100, true, "31/01/2024".toDate())
            val toInsertAtMiddle = tx("test50", 100, true, "28/01/2024".toDate())

            bookTransactionUseCase.handle(BookTransactionCommand(ctx.userId, ctx.booklet.label, toInsertAtFirst)).assertSuccess()
            bookTransactionUseCase.handle(BookTransactionCommand(ctx.userId, ctx.booklet.label, toInsertAtMiddle)).assertSuccess()
            bookTransactionUseCase.handle(BookTransactionCommand(ctx.userId, ctx.booklet.label, toInsertAtLast)).assertSuccess()

            val transactions = transactionState.getStates().find { it.id.userId == ctx.userId && it.id.bookletId == ctx.booklet.id }
                ?.transactions

            transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                .asNullableDomainResult()
                .assertContainsAtPosition(0, toInsertAtFirst)
                .assertContainsAtPosition(6, toInsertAtLast)
                .assertContainsAtPosition(5, toInsertAtMiddle)
        }

        @Test
        fun `When I add a transaction in the middle of a booklet that already have some, its position should be coherent regarding the date`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("test1", 100, true, "01/01/2024".toDate()),
                tx("test2", 100, true, "02/01/2024".toDate()),
                tx("tes3", 100, true, "03/01/2024".toDate()),
                tx("test4", 100, true, "05/01/2024".toDate())
            )))

            val transactionToSave = tx("test", 100, true, "04/01/2024".toDate())

            bookTransactionUseCase.handle(BookTransactionCommand(ctx.userId, ctx.booklet.label, transactionToSave))
                .assertSuccess()

            val transactions = transactionState.getStates().find { it.id.userId == ctx.userId && it.id.bookletId == ctx.booklet.id }
                ?.transactions

            transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                .asNullableDomainResult()
                .assertContainsAtPosition(3, transactionToSave)
        }

        @Test
        fun `When I add a transaction with a same date as existing some, it should be in the last position of them`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("test1", 100, true, "01/01/2024".toDate()),
                tx("test2", 100, true, "02/01/2024".toDate()),
                tx("tes3", 100, true, "02/01/2024".toDate()),
                tx("tes10", 100, true, "02/01/2024".toDate()),
                tx("test4", 100, true, "03/01/2024".toDate())
            )))
            val transactionToSave = tx("test", 100, true, "02/01/2024".toDate())
            bookTransactionUseCase.handle(BookTransactionCommand(ctx.userId, ctx.booklet.label, transactionToSave))
                .assertSuccess()
            val transactions = transactionState.getStates().find { it.id.userId == ctx.userId && it.id.bookletId == ctx.booklet.id }
                ?.transactions

            transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                .asNullableDomainResult()
                .assertContainsAtPosition(4, transactionToSave)
        }

        @Test
        fun `Giving a user with a transaction, when booking a transaction that is older that the others, it should have its position to 0`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("test1", 100, true, "01/01/2024".toDate()),
                tx("test2", 100, true, "02/01/2024".toDate()),
                tx("tes3", 100, true, "02/01/2024".toDate()),
                tx("test4", 100, true, "03/01/2024".toDate())
            )))
            val transactionToSave = tx("test", 100, true, "23/12/2023".toDate())

            bookTransactionUseCase.handle(BookTransactionCommand(ctx.userId, ctx.booklet.label, transactionToSave))
                .assertSuccess()
        }
    }

    @Nested
    inner class RetrieveTransactionsByMonthAndYearFeature {
        @Test
        fun `As a user with existing transactions, I should retrieve them ordering by date and position`() {
            val ctx = scenario.withUser().withBooklet()
            val t1 = tx("test1", 100, true, "01/01/2024".toDate())
            val t2 = tx("test2", 100, true, "02/01/2024".toDate())
            val t3 = tx("tes3", 100, true, "02/01/2024".toDate())
            val t4 = tx("test4", 100, true, "03/01/2024".toDate())
            val t5 = tx("test4", 100, true, "03/01/2024".toDate())
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                t1, t2, t4, t3, t5,
                tx("test5", 100, true, "01/02/2024".toDate()),
                tx("test6", 100, true, "01/02/2024".toDate()),
            )))

            val response = act { retrieveTransactionsByMonthAndYearUseCase.handle(RetrieveTransactionsByMonthAndYearQuery(ctx.userId, Month.JANUARY, 2024, ctx.booklet.label)) }

            then(response) {
                map { it.size } shouldBe 5
                assertTrue { all { it.date.month == Month.JANUARY } }
                assertEquals(listOf(t1, t2, t3, t4, t5))
            }
        }
    }

    @Nested
    inner class EditTransactionFeature {

        @Test
        fun `Giving an existing transaction, I should correctly edit label, amount and date from it`() {
            val ctx = scenario.withUser().withBooklet()
            val elements = tx("test1", 100, true, "01/02/2024".toDate())
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(elements)))
            val expectedLabel = "test1.0"
            val expectedAmount = 105.toAmount()
            val expectedDate = "02/02/2024".toDate()

            act { editTransactionUseCase.handle(EditTransactionCommand(ctx.userId, ctx.booklet.id!!, elements.copy(label = "test1.0", amount = 105.toAmount(), date = "02/02/2024".toDate()))) }

            val actualTransaction = transactionState.getStates().find { it.id.userId == ctx.userId && it.id.bookletId == ctx.booklet.id }
                ?.transactions?.find { tr -> tr.id == elements.id }
            assertEquals(expectedLabel, actualTransaction?.label)
            assertEquals(expectedAmount, actualTransaction?.amount)
            assertEquals(expectedDate, actualTransaction?.date)
        }

        @Test
        fun `Giving existing transactions, when one is edited with a older date, all the position after should still be coherent`() {
            val t1 = tx("test1", 100, true, "01/01/2024".toDate())
            val t2 = tx("test2", 100, true, "02/01/2024".toDate())
            val t3 = tx("test3", 100, true, "02/01/2024".toDate())
            val t4 = tx("test4", 100, true, "03/01/2024".toDate())
            val t5 = tx("test5", 100, true, "03/01/2024".toDate())
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(t1, t2, t3, t4, t5)))

            act { editTransactionUseCase.handle(EditTransactionCommand(ctx.userId, ctx.booklet.id!!, t5.copy(date = "31/12/2023".toDate()))).assertSuccess() }

            val transactions = transactionState.getStates().find { it.id.userId == ctx.userId && it.id.bookletId == ctx.booklet.id }
                ?.transactions
            transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                .asNullableDomainResult()
                .assertEqualsAtPosition(0, t5.label) { label }
                .assertEqualsAtPosition(1, t1.label) { label }
                .assertEqualsAtPosition(2, t2.label) { label }
                .assertEqualsAtPosition(3, t3.label) { label }
                .assertEqualsAtPosition(4, t4.label) { label }
        }

        @Test
        fun `Giving existing transactions, when one is edited with a more recent new date, all the position after should still be coherent`() {
            val t1 = tx("test1", 100, true, "01/01/2024".toDate())
            val t2 = tx("test2", 100, true, "02/01/2024".toDate())
            val t3 = tx("test3", 100, true, "02/01/2024".toDate())
            val t4 = tx("test4", 100, true, "03/01/2024".toDate())
            val t5 = tx("test5", 100, true, "04/01/2024".toDate())
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(t1, t2, t3, t4, t5)))

            act { editTransactionUseCase.handle(EditTransactionCommand(ctx.userId, ctx.booklet.id!!, t1.copy(date = "02/01/2024".toDate()))).assertSuccess() }

            val transactions = transactionState.getStates().find { it.id.userId == ctx.userId && it.id.bookletId == ctx.booklet.id }
                ?.transactions
            assertEquals(5, transactions?.size)

            transactions.sortedByDate()
                .assertEqualsAtPosition(0, t2.label) { label }
                .assertEqualsAtPosition(1, t3.label) { label }
                .assertEqualsAtPosition(2, t1.label) { label }
        }

        @Test
        fun `Giving a user that save a transaction, when we edit it, the new amount of the booklet should take in count`() {
            val ctx = scenario.withUser().withBooklet()
            val transaction = tx("test0", 100, true, "02/01/2024".toDate())
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(transaction)))
            val transaction2 = tx("test1", 100, true, "02/01/2024".toDate())
            bookTransactionUseCase.handle(BookTransactionCommand(ctx.userId, ctx.booklet.label, transaction2))

            act { editTransactionUseCase.handle(EditTransactionCommand(ctx.userId, ctx.booklet.id!!, transaction2.copy(amount = 105.toAmount()))).assertSuccess() }

            val actualBooklet = bookletState.getStates().find { it.userId == ctx.userId }?.booklets?.find { it.id == ctx.booklet.id }
            assertEquals(205.toAmount(), actualBooklet!!.amount)
        }

        @Test
        fun `edit transaction from another user's booklet must return not found and leave it unchanged`() {
            val owner = scenario.withUser().withBooklet()
            val transaction = tx("test0", 100, true, "02/01/2024".toDate())
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(owner.userId, owner.booklet.id!!), mutableListOf(transaction)))
            val intruder = anIntruder()

            val result = act {
                editTransactionUseCase.handle(
                    EditTransactionCommand(intruder.userId, owner.booklet.id!!, transaction.copy(label = "hacked"))
                )
            }

            then(result) { assertFailure(ResultState.NOT_FOUND) }
            val actualTransaction = transactionState.getStates().find { it.id.userId == owner.userId && it.id.bookletId == owner.booklet.id }
                ?.transactions?.find { it.id == transaction.id }
            assertEquals("test0", actualTransaction?.label)
        }

        @Test
        fun `Giving a user with existing transaction, it should be retrieving it by its ID`() {
            val ctx = scenario.withUser().withBooklet()
            val toInsert = tx("test1", 100, true, "01/01/2024".toDate())
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(toInsert)))

            val result = act { findTransactionByIdUseCase.handle(FindTransactionByIdQuery(ctx.userId, toInsert.id!!)) }

            then(result) { assertTrue { this.label == toInsert.label && this.amount == toInsert.amount } }
        }
    }

    @Nested
    inner class BookingPreviewTransaction {
        @Test
        fun `booking a preview transaction should not change the real amount of a booklet`() {
            val ctx = scenario.withUser().withBooklet()
            val transactionPreviewTest = Transaction(UUID.randomUUID(), "test#0", "01/01/2024".toDate(), 100.toAmount(), true, isPreview = true)

            act { bookTransactionUseCase.handle(BookTransactionCommand(ctx.userId, ctx.booklet.label, transactionPreviewTest)).assertSuccess() }

            val actualBooklet = bookletState.getStates().find { it.userId == ctx.userId }?.booklets?.find { it.id == ctx.booklet.id }
            val actualAmount = actualBooklet?.amount ?: 10.toAmount().negate()
            org.junit.jupiter.api.assertAll(
                { assertEquals(0.toAmount(), actualAmount) }
            )
        }
    }

    @Nested
    inner class DeleteByIdFeature {
        @Test
        fun `Giving a user with existing transaction, when we delete it, the new amount of the booklet should take in count`() {
            val ctx = scenario.withUser().withBooklet()
            val transaction = tx("test0", 100, true, "02/01/2024".toDate())
            val transaction2 = tx("test2", 100, true, "02/01/2024".toDate())
            val transaction3 = tx("test3", 100, true, "02/01/2024".toDate())
            val transaction4 = tx("test4", 100, true, "02/01/2024".toDate())
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                transaction, transaction2, transaction3, transaction4
            )))

            act {
                deleteTransactionsByIdsUseCase.handle(DeleteTransactionsByIdsCommand(ctx.userId, ctx.booklet.id!!, listOf(
                    transaction.id!!, transaction2.id!!
                ))).assertSuccess()
            }

            val transactions = transactionState.getStates().find { it.id.userId == ctx.userId && it.id.bookletId == ctx.booklet.id }
                ?.transactions
            assertNull(transactions!!.find { it.label == "test0" })
            assertNull(transactions.find { it.label == "test2" })
        }

        @Test
        fun `delete transaction with invalid booklet must return not found`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act {
                deleteTransactionsByIdsUseCase.handle(DeleteTransactionsByIdsCommand(ctx.userId, UUID.randomUUID(), listOf(
                    UUID.randomUUID(), UUID.randomUUID()
                )))
            }

            then(result) { assertFailure(ResultState.BOOKLET_NOT_FOUND) }
        }

        @Test
        fun `delete transactions from another user's booklet must return not found and change nothing`() {
            val owner = scenario.withUser().withBooklet()
            val transaction = tx("test0", 100, true, "02/01/2024".toDate())
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(owner.userId, owner.booklet.id!!), mutableListOf(transaction)))
            val intruder = anIntruder()

            val result = act {
                deleteTransactionsByIdsUseCase.handle(
                    DeleteTransactionsByIdsCommand(intruder.userId, owner.booklet.id!!, listOf(transaction.id!!))
                )
            }

            then(result) { assertFailure(ResultState.BOOKLET_NOT_FOUND) }
            val transactions = transactionState.getStates().find { it.id.userId == owner.userId && it.id.bookletId == owner.booklet.id }
                ?.transactions
            assertNotNull(transactions?.find { it.id == transaction.id })
        }

        @Test
        fun `delete preview regular transaction should exclude month for regular transaction`() {
            val ctx = scenario.withUser().withBooklet()
            val regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-1")
            val previewTransaction = tx("test-preview", 100, true, "02/01/2024".toDate(), isPreview = true)
                .copy(regularTransactionId = regularTransactionId)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(previewTransaction)))

            act { deleteTransactionsByIdsUseCase.handle(DeleteTransactionsByIdsCommand(ctx.userId, ctx.booklet.id!!, listOf(previewTransaction.id!!))).assertSuccess() }

            val tracker = factory.trackerRepository().findTracker(regularTransactionId, ctx.booklet.id!!)
            assertNotNull(tracker)
            assertTrue(tracker!!.excludedMonths.contains(YearMonth.of(2024, Month.JANUARY)))
        }
    }

    @Nested
    inner class ConfirmPreviewTransactionTest {
        @Test
        fun `confirm preview conversion should be success`() {
            val ctx = scenario.withUser().withBooklet()
            val transactionPreviewTest = tx("test#0", 100, true, "01/01/2024".toDate(), isPreview = true)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(transactionPreviewTest)))

            val result = act { confirmPreviewTransactionUseCase.handle(ConfirmPreviewTransactionCommand(ctx.userId, ctx.booklet.id!!, transactionPreviewTest.id!!, null, null)) }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Confirm preview transaction with invalid booklet id must resolve booklet not found`() {
            val ctx = scenario.withUser().withBooklet()
            val transactionPreviewTest = tx("test#0", 100, true, "01/01/2024".toDate(), isPreview = true)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(transactionPreviewTest)))

            val result = act { confirmPreviewTransactionUseCase.handle(ConfirmPreviewTransactionCommand(ctx.userId, UUID.randomUUID(), transactionPreviewTest.id!!, null, null)) }

            then(result) {
                assertFailure(ResultState.BOOKLET_NOT_FOUND)
                assertEquals("domain.transaction.confirm.booklet_not_found", errorInfo?.key)
            }
        }

        @Test
        fun `confirm preview transaction with a transaction that is not found must return to not found`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { confirmPreviewTransactionUseCase.handle(ConfirmPreviewTransactionCommand(ctx.userId, ctx.booklet.id!!, UUID.randomUUID(), null, null)) }

            then(result) { assertFailure(ResultState.BOOKLET_NOT_FOUND) }
        }

        @Test
        fun `confirm preview with new amount should update transaction and booklet amount`() {
            val ctx = scenario.withUser().withBooklet()
            val transactionPreviewTest = tx("test#1", 100, true, "01/01/2024".toDate(), isPreview = true)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(transactionPreviewTest)))
            val newAmount = 150.toAmount()

            act { confirmPreviewTransactionUseCase.handle(ConfirmPreviewTransactionCommand(ctx.userId, ctx.booklet.id!!, transactionPreviewTest.id!!, newAmount, null)).assertSuccess() }

            val transactions = transactionState.getStates().find { it.id.userId == ctx.userId && it.id.bookletId == ctx.booklet.id }
                ?.transactions
            val updated = transactions?.find { it.id == transactionPreviewTest.id }
            assertNotNull(updated)
            assertFalse(updated!!.isPreview)
            assertEquals(newAmount, updated.amount)

            val actualBooklet = bookletState.getStates().find { it.userId == ctx.userId }?.booklets?.find { it.id == ctx.booklet.id }
            assertEquals(newAmount, actualBooklet?.amount)
        }

        @Test
        fun `confirm preview transaction from another user's booklet must return not found and leave it as preview`() {
            val owner = scenario.withUser().withBooklet()
            val transactionPreviewTest = tx("test#0", 100, true, "01/01/2024".toDate(), isPreview = true)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(owner.userId, owner.booklet.id!!), mutableListOf(transactionPreviewTest)))
            val intruder = anIntruder()

            val result = act {
                confirmPreviewTransactionUseCase.handle(
                    ConfirmPreviewTransactionCommand(intruder.userId, owner.booklet.id!!, transactionPreviewTest.id!!, null, null)
                )
            }

            then(result) { assertFailure(ResultState.BOOKLET_NOT_FOUND) }
            val actual = transactionState.getStates().find { it.id.userId == owner.userId && it.id.bookletId == owner.booklet.id }
                ?.transactions?.find { it.id == transactionPreviewTest.id }
            assertTrue(actual!!.isPreview)
        }

        @Test
        fun `confirm preview with new date should update transaction date`() {
            val ctx = scenario.withUser().withBooklet()
            val transactionPreviewTest = tx("test#2", 100, true, "01/01/2024".toDate(), isPreview = true)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(transactionPreviewTest)))
            val newDate = "12/01/2024".toDate()

            act { confirmPreviewTransactionUseCase.handle(ConfirmPreviewTransactionCommand(ctx.userId, ctx.booklet.id!!, transactionPreviewTest.id!!, null, newDate)).assertSuccess() }

            val transactions = transactionState.getStates().find { it.id.userId == ctx.userId && it.id.bookletId == ctx.booklet.id }
                ?.transactions
            val updated = transactions?.find { it.id == transactionPreviewTest.id }
            assertNotNull(updated)
            assertFalse(updated!!.isPreview)
            assertEquals(newDate, updated.date)
        }
    }

    @Nested
    inner class ConfirmVirtualTransactionTest {

        private val confirmVirtualTransactionUseCase: ConfirmVirtualTransactionUseCase = factory.confirmVirtualTransactionService

        @Test
        fun `shouldPersistRealTransactionAndExcludeSourceMonth_whenConfirmingVirtualTransactionWithUnchangedDate`() {
            val ctx = scenario.withUser().withBooklet()

            val regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-virtual-1")

            val result = act {
                confirmVirtualTransactionUseCase.handle(
                    ConfirmVirtualTransactionCommand(
                        userId = ctx.userId,
                        bookletId = ctx.booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        sourceMonth = 5,
                        sourceYear = 2026,
                        label = "Salaire",
                        amount = 3000.toAmount(),
                        date = LocalDate.of(2026, 5, 15),
                        isIncome = true
                    )
                )
            }

            then(result) {
                assertSuccess()
                onSuccess { transactionResult ->
                    assertFalse(transactionResult.transaction.isPreview)
                    assertEquals("Salaire", transactionResult.transaction.label)
                    assertEquals(3000.toAmount(), transactionResult.transaction.amount)
                    assertEquals(LocalDate.of(2026, 5, 15), transactionResult.transaction.date)
                }
            }

            val tracker = factory.trackerRepository().findTracker(regularTransactionId, ctx.booklet.id!!)
            assertNotNull(tracker)
            assertTrue(tracker!!.excludedMonths.contains(YearMonth.of(2026, Month.MAY)))
        }

        @Test
        fun `shouldPersistTransactionWithNewDateAndExcludeOnlySourceMonth_whenDateChangedToDifferentMonth`() {
            val ctx = scenario.withUser().withBooklet()

            val regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-virtual-2")

            val result = act {
                confirmVirtualTransactionUseCase.handle(
                    ConfirmVirtualTransactionCommand(
                        userId = ctx.userId,
                        bookletId = ctx.booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        sourceMonth = 5,
                        sourceYear = 2026,
                        label = "Salaire",
                        amount = 3000.toAmount(),
                        date = LocalDate.of(2026, 4, 20),
                        isIncome = true
                    )
                )
            }

            then(result) {
                assertSuccess()
                onSuccess { transactionResult ->
                    assertEquals(LocalDate.of(2026, 4, 20), transactionResult.transaction.date)
                }
            }

            val tracker = factory.trackerRepository().findTracker(regularTransactionId, ctx.booklet.id!!)
            assertNotNull(tracker)
            assertTrue(tracker!!.excludedMonths.contains(YearMonth.of(2026, Month.MAY)))
            assertFalse(tracker.excludedMonths.contains(YearMonth.of(2026, Month.APRIL)))
        }

        @Test
        fun `shouldReturnBookletNotFound_whenBookletDoesNotExist`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act {
                confirmVirtualTransactionUseCase.handle(
                    ConfirmVirtualTransactionCommand(
                        userId = ctx.userId,
                        bookletId = UUID.randomUUID(),
                        regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-virtual-3"),
                        sourceMonth = 5,
                        sourceYear = 2026,
                        label = "Salaire",
                        amount = 3000.toAmount(),
                        date = LocalDate.of(2026, 5, 15),
                        isIncome = true
                    )
                )
            }

            then(result) { assertFailure(ResultState.BOOKLET_NOT_FOUND) }
        }

        @Test
        fun `shouldReturnBookletNotFound_whenBookletBelongsToAnotherUser`() {
            val owner = scenario.withUser().withBooklet()
            val intruder = anIntruder()
            val regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-virtual-intruder")

            val result = act {
                confirmVirtualTransactionUseCase.handle(
                    ConfirmVirtualTransactionCommand(
                        userId = intruder.userId,
                        bookletId = owner.booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        sourceMonth = 5,
                        sourceYear = 2026,
                        label = "Salaire",
                        amount = 3000.toAmount(),
                        date = LocalDate.of(2026, 5, 15),
                        isIncome = true
                    )
                )
            }

            then(result) { assertFailure(ResultState.BOOKLET_NOT_FOUND) }
            val transactions = transactionState.getStates()
                .find { it.id.userId == owner.userId && it.id.bookletId == owner.booklet.id }
                ?.transactions
            assertTrue(transactions.isNullOrEmpty())
            val tracker = factory.trackerRepository().findTracker(regularTransactionId, owner.booklet.id!!)
            assertNull(tracker)
        }

        @Test
        fun `shouldCreateTrackerAndExcludeMonth_whenNoTrackerExistsForBooklet`() {
            val ctx = scenario.withUser().withBooklet()

            val regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-virtual-4")
            val trackerBefore = factory.trackerRepository().findTracker(regularTransactionId, ctx.booklet.id!!)
            assertNull(trackerBefore)

            val result = act {
                confirmVirtualTransactionUseCase.handle(
                    ConfirmVirtualTransactionCommand(
                        userId = ctx.userId,
                        bookletId = ctx.booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        sourceMonth = 5,
                        sourceYear = 2026,
                        label = "Salaire",
                        amount = 3000.toAmount(),
                        date = LocalDate.of(2026, 5, 15),
                        isIncome = true
                    )
                )
            }

            then(result) { assertSuccess() }

            val tracker = factory.trackerRepository().findTracker(regularTransactionId, ctx.booklet.id!!)
            assertNotNull(tracker)
            assertTrue(tracker!!.excludedMonths.contains(YearMonth.of(2026, Month.MAY)))
        }

        @Test
        fun `shouldPersistTransactionWithTagLabel_whenTagLabelIsProvided`() {
            val ctx = scenario.withUser().withBooklet()

            val regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-virtual-5")

            val result = act {
                confirmVirtualTransactionUseCase.handle(
                    ConfirmVirtualTransactionCommand(
                        userId = ctx.userId,
                        bookletId = ctx.booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        sourceMonth = 5,
                        sourceYear = 2026,
                        label = "Salaire",
                        amount = 3000.toAmount(),
                        date = LocalDate.of(2026, 5, 15),
                        isIncome = true,
                        tag = Tag.Default("Alimentation & Restaurant")
                    )
                )
            }

            then(result) {
                assertSuccess()
                onSuccess { transactionResult ->
                    assertNotNull(transactionResult.transaction.tag)
                    assertEquals("Alimentation & Restaurant", transactionResult.transaction.tag!!.label)
                }
            }
        }
    }
}

fun String.toDate(): LocalDate = LocalDate.parse(this, DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun MutableList<Transaction>?.sortedByDate(): Result<List<Transaction>> {
    return this?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
        .asNullableDomainResult()
}
