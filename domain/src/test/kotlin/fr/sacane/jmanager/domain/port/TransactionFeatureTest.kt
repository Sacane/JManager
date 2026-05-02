package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.*
import fr.sacane.jmanager.domain.fake.BookletsByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserBooklet
import fr.sacane.jmanager.domain.fake.IdBookletByTransaction
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.input.transaction.*
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

fun <T> T.asSingleton(): List<T> = listOf(this)

class TransactionFeatureTest: FeatureTest() {

    companion object{
        private val transactionState: State<IdBookletByTransaction> = FakeFactory.fakeTransactionRepository()
        private val bookletState: State<BookletsByOwner> = FakeFactory.bookletState()
        private val bookTransactionUseCase: BookTransactionUseCase = FakeFactory.bookTransactionService
        private val retrieveTransactionsByMonthAndYearUseCase: RetrieveTransactionsByMonthAndYearUseCase = FakeFactory.retrieveTransactionsByMonthAndYearService
        private val editTransactionUseCase: EditTransactionUseCase = FakeFactory.editTransactionService
        private val findTransactionByIdUseCase: FindTransactionByIdUseCase = FakeFactory.findTransactionByIdService
        private val deleteTransactionsByIdsUseCase: DeleteTransactionsByIdsUseCase = FakeFactory.deleteTransactionsByIdsService
        private val confirmPreviewTransactionUseCase: ConfirmPreviewTransactionUseCase = FakeFactory.confirmPreviewTransactionService
    }

    @Nested
    inner class SaveTransactionInBookletFeatureTest {
        @Test
        fun `When I add a new transaction, it should persist it and update the booklet amount when its income and outcome`() {
            launchWithUserId {
                val transactionToSave = generateTransaction("test", 100.toAmount(), true)
                val transactionToSave2 = generateTransaction("test", 50.toAmount(), false)
                bookTransactionUseCase.handle(BookTransactionCommand(userId, booklet.label, transactionToSave))
                    .assertTrue {
                        this.transaction.amount == transactionToSave.amount && this.transaction.label == transactionToSave.label
                    }
                bookTransactionUseCase.handle(BookTransactionCommand(userId, booklet.label, transactionToSave2))
                    .assertTrue {
                        this.transaction.amount == transactionToSave2.amount && this.transaction.label == transactionToSave2.label
                    }

                val bookletStates = bookletState.getStates()
                val bookletByOwnerTarget = bookletStates.find { it.userId == userId }
                val bookletExpected = bookletByOwnerTarget?.booklets?.find { it.id == booklet.id }
                assertNotNull(bookletExpected)
                assertEquals(Amount(50), bookletExpected?.amount)
            }
        }

        @Test
        fun `When I add a transaction in a booklet that already have some, its position should be coherent regarding the date`() {
            launchWithUserId {
                initTransactions(listOf(
                    generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate()),
                    generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("tes3", 100.toAmount(), true, "03/01/2024".toDate()),
                    generateTransaction("test4", 100.toAmount(), true, "04/01/2024".toDate())
                ))

                val toInsertAtFirst = generateTransaction("test0", 100.toAmount(), true, "31/12/2023".toDate())
                val toInsertAtLast = generateTransaction("test100", 100.toAmount(), true, "31/01/2024".toDate())
                val toInsertAtMiddle = generateTransaction("test50", 100.toAmount(), true, "28/01/2024".toDate())

                bookTransactionUseCase.handle(BookTransactionCommand(userId, booklet.label, toInsertAtFirst)).assertSuccess()
                bookTransactionUseCase.handle(BookTransactionCommand(userId, booklet.label, toInsertAtMiddle)).assertSuccess()
                bookTransactionUseCase.handle(BookTransactionCommand(userId, booklet.label, toInsertAtLast)).assertSuccess()

                val state = transactionState.getStates()
                val transactions = state.find { it.id.userId == userId && it.id.bookletId == booklet.id }
                    ?.transactions

                transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asNullableDomainResult()
                    .assertContainsAtPosition(0, toInsertAtFirst)
                    .assertContainsAtPosition(6, toInsertAtLast)
                    .assertContainsAtPosition(5, toInsertAtMiddle)
            }
        }

        @Test
        fun `When I add a transaction in the middle of a booklet that already have some, its position should be coherent regarding the date`() {
            launchWithUserId {
                initTransactions(listOf(
                    generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate()),
                    generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("tes3", 100.toAmount(), true, "03/01/2024".toDate()),
                    generateTransaction("test4", 100.toAmount(), true, "05/01/2024".toDate())
                ))

                val transactionToSave = generateTransaction("test", 100.toAmount(), true, "04/01/2024".toDate())

                bookTransactionUseCase.handle(BookTransactionCommand(userId, booklet.label, transactionToSave))
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.bookletId == booklet.id }
                    ?.transactions

                transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asNullableDomainResult()
                    .assertContainsAtPosition(3, transactionToSave)
            }
        }

        @Test
        fun `When I add a transaction with a same date as existing some, it should be in the last position of them`() {
            launchWithUserId {
                initTransactions(mutableListOf(
                    generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate()),
                    generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("tes3", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("tes10", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate())
                ))
                val transactionToSave = generateTransaction("test", 100.toAmount(), true, "02/01/2024".toDate())
                bookTransactionUseCase.handle(BookTransactionCommand(userId, booklet.label, transactionToSave))
                    .assertSuccess()
                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.bookletId == booklet.id }
                    ?.transactions

                transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asNullableDomainResult()
                    .assertContainsAtPosition(4, transactionToSave)
            }
        }
        @Test
        fun `Giving a user with a transaction, when booking a transaction that is older that the others, it should have its position to 0`() {
            launchWithUserId {
                initTransactions( mutableListOf(
                    generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate()),
                    generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("tes3", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate())
                ))
                val transactionToSave = generateTransaction("test", 100.toAmount(), true, "23/12/2023".toDate())

                bookTransactionUseCase.handle(BookTransactionCommand(userId, booklet.label, transactionToSave))
                    .assertSuccess()
            }
        }
    }

    @Nested
    inner class RetrieveTransactionsByMonthAndYearFeature {
        @Test
        fun `As a user with existing transactions, I should retrieve them ordering by date and position`() {
            launchWithUserId {
                val t1 = generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate())
                val t2 = generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate())
                val t3 = generateTransaction("tes3", 100.toAmount(), true, "02/01/2024".toDate())
                val t4 = generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate())
                val t5 = generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate())
                initTransactions(listOf(
                    t1, t2, t4, t3, t5,
                    generateTransaction("test5", 100.toAmount(), true, "01/02/2024".toDate()),
                    generateTransaction("test6", 100.toAmount(), true, "01/02/2024".toDate()),
                ))
                val response = retrieveTransactionsByMonthAndYearUseCase.handle(RetrieveTransactionsByMonthAndYearQuery(userId, Month.JANUARY, 2024, booklet.label))
                response.map { it.size } shouldBe 5
                response.assertTrue {
                     all { it.date.month == Month.JANUARY }
                }
                response.assertEquals(listOf(t1, t2, t3, t4, t5))
            }
        }
    }
    @Nested
    inner class EditTransactionFeature {

        @Test
        fun `Giving an existing transaction, I should correctly edit label, amount and date from it`() {
            launchWithUserId {
                val elements = generateTransaction("test1", 100.toAmount(), true, "01/02/2024".toDate())
                initTransactions(elements.asSingleton())
                val expectedLabel = "test1.0"
                val expectedAmount = 105.toAmount()
                val expectedDate = "02/02/2024".toDate()
                editTransactionUseCase.handle(EditTransactionCommand(
                    userId, booklet.id!!, elements.copy(label = "test1.0", amount = 105.toAmount(), date = "02/02/2024".toDate())
                ))

                val actualTransaction = transactionState.getStates().find { it.id.userId == userId && it.id.bookletId == booklet.id }
                    ?.transactions?.find { tr -> tr.id == elements.id }

                assertEquals(expectedLabel, actualTransaction?.label)
                assertEquals(expectedAmount, actualTransaction?.amount)
                assertEquals(expectedDate, actualTransaction?.date)
            }
        }

        @Test
        fun `Giving existing transactions, when one is edited with a older date, all the position after should still be coherent`() {
            val t1 = generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate())
            val t2 = generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate())
            val t3 = generateTransaction("test3", 100.toAmount(), true, "02/01/2024".toDate())
            val t4 = generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate())
            val t5 = generateTransaction("test5", 100.toAmount(), true, "03/01/2024".toDate())
            launchWithUserId {
                initTransactions(listOf(t1, t2, t3, t4, t5))

                editTransactionUseCase.handle(EditTransactionCommand(userId, booklet.id!!, t5.copy(date = "31/12/2023".toDate())))
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.bookletId == booklet.id }
                    ?.transactions
                transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asNullableDomainResult()
                    .assertEqualsAtPosition(0, t5.label) {label}
                    .assertEqualsAtPosition(1, t1.label) {label}
                    .assertEqualsAtPosition(2, t2.label) {label}
                    .assertEqualsAtPosition(3, t3.label) {label}
                    .assertEqualsAtPosition(4, t4.label) {label}
            }
        }
        @Test
        fun `Giving existing transactions, when one is edited with a more recent new date, all the position after should still be coherent`() {
            val t1 = generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate())
            val t2 = generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate())
            val t3 = generateTransaction("test3", 100.toAmount(), true, "02/01/2024".toDate())
            val t4 = generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate())
            val t5 = generateTransaction("test5", 100.toAmount(), true, "04/01/2024".toDate())
            launchWithUserId {
                initTransactions(listOf(t1, t2, t3, t4, t5))
                editTransactionUseCase.handle(EditTransactionCommand(userId, booklet.id!!, t1.copy(date = "02/01/2024".toDate())))
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.bookletId == booklet.id }
                    ?.transactions
                assertEquals(5, transactions?.size)

                transactions.sortedByDate()
                    .assertEqualsAtPosition(0, t2.label) { label }
                    .assertEqualsAtPosition(1, t3.label) { label }
                    .assertEqualsAtPosition(2, t1.label) { label }
            }
        }

        @Test
        fun `Giving a user that save a transaction, when we edit it, the new amount of the booklet should take in count`() {
            launchWithUserId {
                val transaction = generateTransaction("test0", 100.toAmount(), true, "02/01/2024".toDate())
                initTransactions(listOf(
                    transaction
                ))
                val transaction2 = generateTransaction("test1", 100.toAmount(), true, "02/01/2024".toDate())
                bookTransactionUseCase.handle(BookTransactionCommand(userId, booklet.label, transaction2))
                editTransactionUseCase.handle(EditTransactionCommand(userId, booklet.id!!, transaction2.copy(amount = 105.toAmount())))
                    .assertSuccess()

                val actualBooklet = bookletState.getStates().find { it.userId == userId }?.booklets?.find { it.id == booklet.id }

                assertEquals(205.toAmount(), actualBooklet!!.amount)
            }
        }
        @Test
        fun `Giving a user with existing transaction, it should be retrieving it by its ID`() {
            launchWithUserId {
                val toInsert = generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate())
                initTransactions(toInsert.asSingleton())
                findTransactionByIdUseCase.handle(FindTransactionByIdQuery(userId, toInsert.id!!))
                    .assertTrue { this.label == toInsert.label && this.amount == toInsert.amount }
            }
        }
    }
    @Nested
    inner class BookingPreviewTransaction {
        @Test
        fun `booking a preview transaction should not change the real amount of a booklet`() {
            launchWithUserId {
                val transactionPreviewTest = Transaction(UUID.randomUUID(), "test#0", "01/01/2024".toDate(), 100.toAmount(), true, isPreview = true)
                bookTransactionUseCase.handle(BookTransactionCommand(userId, booklet.label, transactionPreviewTest))
                    .assertSuccess()
                val actualBooklet = bookletState.getStates().find { it.userId == userId }?.booklets?.find { it.id == booklet.id }
                val actualAmount = actualBooklet?.amount ?: 10.toAmount().negate()

                org.junit.jupiter.api.assertAll(
                    { assertEquals(0.toAmount(), actualAmount) }
                )
            }
        }
    }
    @Nested
    inner class DeleteByIdFeature {
        @Test
        fun `Giving a user with existing transaction, when we delete it, the new amount of the booklet should take in count`() {
            launchWithUserId {
                val transaction = generateTransaction("test0", 100.toAmount(), true, "02/01/2024".toDate())
                val transaction2 = generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate())
                val transaction3 = generateTransaction("test3", 100.toAmount(), true, "02/01/2024".toDate())
                val transaction4 = generateTransaction("test4", 100.toAmount(), true, "02/01/2024".toDate())
                initTransactions(listOf(
                    transaction, transaction2, transaction3, transaction4
                ))
                deleteTransactionsByIdsUseCase.handle(DeleteTransactionsByIdsCommand(userId, booklet.id!!, listOf(
                    transaction.id!!, transaction2.id!!
                )))
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.bookletId == booklet.id }
                    ?.transactions

                assertNull(transactions!!.find { it.label == "test0" })
                assertNull(transactions.find { it.label == "test2" })
            }
        }

        @Test
        fun `delete transaction with invalid booklet must return not found`() {
            launchWithUserId {
                deleteTransactionsByIdsUseCase.handle(DeleteTransactionsByIdsCommand(userId, UUID.randomUUID(), listOf(
                    UUID.randomUUID(), UUID.randomUUID()
                )))
                    .assertFailure(ResultState.BOOKLET_NOT_FOUND)
            }

        }
        @Test
        fun `delete preview regular transaction should exclude month for regular transaction`() {
            launchWithUserId {
                val regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-1")
                val previewTransaction = generateTransaction("test-preview", 100.toAmount(), true, "02/01/2024".toDate(), isPreview = true)
                    .copy(regularTransactionId = regularTransactionId)

                initTransactions(listOf(previewTransaction))

                deleteTransactionsByIdsUseCase.handle(DeleteTransactionsByIdsCommand(userId, booklet.id!!, listOf(previewTransaction.id!!)))
                    .assertSuccess()

                val tracker = FakeFactory.trackerRepository().findTracker(regularTransactionId, booklet.id)
                assertNotNull(tracker)
                assertTrue(tracker!!.excludedMonths.contains(YearMonth.of(2024, Month.JANUARY)))
            }
        }
    }

    @Nested
    inner class ConfirmPreviewTransactionTest {
        @Test
        fun `confirm preview conversion should be success`() {
            launchWithUserId {
                val transactionPreviewTest =
                    generateTransaction("test#0", 100.toAmount(), true, "01/01/2024".toDate(), isPreview = true)
                transactionState.init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(userId, booklet.id!!),
                            mutableListOf(transactionPreviewTest)
                        )
                    )
                )

                confirmPreviewTransactionUseCase.handle(ConfirmPreviewTransactionCommand(userId,
                    booklet.id,
                    transactionPreviewTest.id!!,
                    null,
                    null
                ))
                    .assertSuccess()
            }
        }
        @Test
        fun `Confirm preview transaction with invalid booklet id must resolve booklet not found`() {
            launchWithUserId {
                val transactionPreviewTest =
                    generateTransaction("test#0", 100.toAmount(), true, "01/01/2024".toDate(), isPreview = true)
                transactionState.init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(userId, booklet.id!!),
                            mutableListOf(transactionPreviewTest)
                        )
                    )
                )

                val result = confirmPreviewTransactionUseCase.handle(ConfirmPreviewTransactionCommand(userId,
                    UUID.randomUUID(),
                    transactionPreviewTest.id!!,
                    null,
                    null
                ))

                result.assertFailure(ResultState.BOOKLET_NOT_FOUND)
                assertEquals("domain.transaction.confirm.booklet_not_found", result.errorInfo?.key)
            }
        }
        @Test
        fun `confirm preview transaction with a transaction that is not found must return to not found`() {
            launchWithUserId {
                confirmPreviewTransactionUseCase.handle(ConfirmPreviewTransactionCommand(userId,
                    booklet.id!!,
                    UUID.randomUUID(),
                    null,
                    null
                )).assertFailure(ResultState.BOOKLET_NOT_FOUND)
            }
        }

        @Test
        fun `confirm preview with new amount should update transaction and booklet amount`() {
            launchWithUserId {
                val transactionPreviewTest = generateTransaction("test#1", 100.toAmount(), true, "01/01/2024".toDate(), isPreview = true)
                transactionState.init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(userId, booklet.id!!),
                            mutableListOf(transactionPreviewTest)
                        )
                    )
                )

                val newAmount = 150.toAmount()

                confirmPreviewTransactionUseCase.handle(ConfirmPreviewTransactionCommand(userId,
                    booklet.id,
                    transactionPreviewTest.id!!,
                    newAmount,
                    null
                )).assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.bookletId == booklet.id }
                    ?.transactions
                val updated = transactions?.find { it.id == transactionPreviewTest.id }
                assertNotNull(updated)
                assertFalse(updated!!.isPreview)
                assertEquals(newAmount, updated.amount)

                val actualBooklet = bookletState.getStates().find { it.userId == userId }?.booklets?.find { it.id == booklet.id }
                assertEquals(newAmount, actualBooklet?.amount)
            }
        }

        @Test
        fun `confirm preview with new date should update transaction date`() {
            launchWithUserId {
                val transactionPreviewTest = generateTransaction("test#2", 100.toAmount(), true, "01/01/2024".toDate(), isPreview = true)
                transactionState.init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(userId, booklet.id!!),
                            mutableListOf(transactionPreviewTest)
                        )
                    )
                )

                val newDate = "12/01/2024".toDate()

                confirmPreviewTransactionUseCase.handle(ConfirmPreviewTransactionCommand(userId,
                    booklet.id,
                    transactionPreviewTest.id!!,
                    null,
                    newDate
                )).assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.bookletId == booklet.id }
                    ?.transactions
                val updated = transactions?.find { it.id == transactionPreviewTest.id }
                assertNotNull(updated)
                assertFalse(updated!!.isPreview)
                assertEquals(newDate, updated.date)
            }
        }
    }

    @Nested
    inner class ConfirmVirtualTransactionTest {

        private val confirmVirtualTransactionUseCase: ConfirmVirtualTransactionUseCase = FakeFactory.confirmVirtualTransactionService

        @Test
        fun `shouldPersistRealTransactionAndExcludeSourceMonth_whenConfirmingVirtualTransactionWithUnchangedDate`() {
            launchWithUserId {
                val regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-virtual-1")

                val result = confirmVirtualTransactionUseCase.handle(
                    ConfirmVirtualTransactionCommand(
                        userId = userId,
                        bookletId = booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        sourceMonth = 5,
                        sourceYear = 2026,
                        label = "Salaire",
                        amount = 3000.toAmount(),
                        date = LocalDate.of(2026, 5, 15),
                        isIncome = true
                    )
                )

                result.assertSuccess()
                result.onSuccess { transactionResult ->
                    assertFalse(transactionResult.transaction.isPreview)
                    assertEquals("Salaire", transactionResult.transaction.label)
                    assertEquals(3000.toAmount(), transactionResult.transaction.amount)
                    assertEquals(LocalDate.of(2026, 5, 15), transactionResult.transaction.date)
                }

                val tracker = FakeFactory.trackerRepository().findTracker(regularTransactionId, booklet.id!!)
                assertNotNull(tracker)
                assertTrue(tracker!!.excludedMonths.contains(YearMonth.of(2026, Month.MAY)))
            }
        }

        @Test
        fun `shouldPersistTransactionWithNewDateAndExcludeOnlySourceMonth_whenDateChangedToDifferentMonth`() {
            launchWithUserId {
                val regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-virtual-2")

                val result = confirmVirtualTransactionUseCase.handle(
                    ConfirmVirtualTransactionCommand(
                        userId = userId,
                        bookletId = booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        sourceMonth = 5,
                        sourceYear = 2026,
                        label = "Salaire",
                        amount = 3000.toAmount(),
                        date = LocalDate.of(2026, 4, 20),
                        isIncome = true
                    )
                )

                result.assertSuccess()
                result.onSuccess { transactionResult ->
                    assertEquals(LocalDate.of(2026, 4, 20), transactionResult.transaction.date)
                }

                val tracker = FakeFactory.trackerRepository().findTracker(regularTransactionId, booklet.id!!)
                assertNotNull(tracker)
                assertTrue(tracker!!.excludedMonths.contains(YearMonth.of(2026, Month.MAY)))
                assertFalse(tracker.excludedMonths.contains(YearMonth.of(2026, Month.APRIL)))
            }
        }

        @Test
        fun `shouldReturnBookletNotFound_whenBookletDoesNotExist`() {
            launchWithUserId {
                val result = confirmVirtualTransactionUseCase.handle(
                    ConfirmVirtualTransactionCommand(
                        userId = userId,
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

                result.assertFailure(ResultState.BOOKLET_NOT_FOUND)
            }
        }

        @Test
        fun `shouldCreateTrackerAndExcludeMonth_whenNoTrackerExistsForBooklet`() {
            launchWithUserId {
                val regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-virtual-4")

                val trackerBefore = FakeFactory.trackerRepository().findTracker(regularTransactionId, booklet.id!!)
                assertNull(trackerBefore)

                val result = confirmVirtualTransactionUseCase.handle(
                    ConfirmVirtualTransactionCommand(
                        userId = userId,
                        bookletId = booklet.id!!,
                        regularTransactionId = regularTransactionId,
                        sourceMonth = 5,
                        sourceYear = 2026,
                        label = "Salaire",
                        amount = 3000.toAmount(),
                        date = LocalDate.of(2026, 5, 15),
                        isIncome = true
                    )
                )

                result.assertSuccess()

                val tracker = FakeFactory.trackerRepository().findTracker(regularTransactionId, booklet.id!!)
                assertNotNull(tracker)
                assertTrue(tracker!!.excludedMonths.contains(YearMonth.of(2026, Month.MAY)))
            }
        }

        @Test
        fun `shouldPersistTransactionWithTagLabel_whenTagLabelIsProvided`() {
            launchWithUserId {
                val regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-virtual-5")

                val result = confirmVirtualTransactionUseCase.handle(
                    ConfirmVirtualTransactionCommand(
                        userId = userId,
                        bookletId = booklet.id!!,
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

                result.assertSuccess()
                result.onSuccess { transactionResult ->
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
