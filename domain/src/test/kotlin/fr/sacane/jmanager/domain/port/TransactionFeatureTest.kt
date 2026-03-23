package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.*
import fr.sacane.jmanager.domain.fake.AccountByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserAccount
import fr.sacane.jmanager.domain.fake.IdUserAccountByTransaction
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
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
        private val transactionState: State<IdUserAccountByTransaction> = FakeFactory.fakeTransactionRepository()
        private val accountState: State<AccountByOwner> = FakeFactory.accountState()
        private val transactionFeature = FakeFactory.transactionFeature

    }

    @Nested
    inner class SaveTransactionInAccountFeatureTest {
        @Test
        fun `When I add a new transaction, it should persist it and update the account amount when its income and outcome`() {
            launchWithConnectedUserInstance {
                val transactionToSave = generateTransaction("test", 100.toAmount(), true)
                val transactionToSave2 = generateTransaction("test", 50.toAmount(), false)
                transactionFeature.bookTransaction(tokenValue, booklet.label, transactionToSave)
                    .assertTrue {
                        this.transaction.amount == transactionToSave.amount && this.transaction.label == transactionToSave.label
                    }
                transactionFeature.bookTransaction(tokenValue, booklet.label, transactionToSave2)
                    .assertTrue {
                        this.transaction.amount == transactionToSave2.amount && this.transaction.label == transactionToSave2.label
                    }

                val accountStates = accountState.getStates()
                val accountByOwnerTarget = accountStates.find { it.userId == user.id }
                val accountExpected = accountByOwnerTarget?.booklet?.find { it.id == booklet.id }
                assertNotNull(accountExpected)
                assertEquals(Amount(50), accountExpected?.amount)
            }
        }

        @Test
        fun `When I add a transaction in an account that already have some, its position should be coherent regarding the date`() {
            launchWithConnectedUserInstance {
                initTransactions(listOf(
                    generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate()),
                    generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("tes3", 100.toAmount(), true, "03/01/2024".toDate()),
                    generateTransaction("test4", 100.toAmount(), true, "04/01/2024".toDate())
                ))

                val toInsertAtFirst = generateTransaction("test0", 100.toAmount(), true, "31/12/2023".toDate())
                val toInsertAtLast = generateTransaction("test100", 100.toAmount(), true, "31/01/2024".toDate())
                val toInsertAtMiddle = generateTransaction("test50", 100.toAmount(), true, "28/01/2024".toDate())

                transactionFeature.bookTransaction(tokenValue, booklet.label, toInsertAtFirst).assertSuccess()
                transactionFeature.bookTransaction(tokenValue, booklet.label, toInsertAtMiddle).assertSuccess()
                transactionFeature.bookTransaction(tokenValue, booklet.label, toInsertAtLast).assertSuccess()

                val state = transactionState.getStates()
                val transactions = state.find { it.id.userId == user.id && it.id.accountId == booklet.id }
                    ?.transactions

                transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asNullableDomainResult()
                    .assertContainsAtPosition(0, toInsertAtFirst)
                    .assertContainsAtPosition(6, toInsertAtLast)
                    .assertContainsAtPosition(5, toInsertAtMiddle)
            }
        }

        @Test
        fun `When I add a transaction in the middle of an account that already have some, its position should be coherent regarding the date`() {
            launchWithConnectedUserInstance {
                initTransactions(listOf(
                    generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate()),
                    generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("tes3", 100.toAmount(), true, "03/01/2024".toDate()),
                    generateTransaction("test4", 100.toAmount(), true, "05/01/2024".toDate())
                ))

                val transactionToSave = generateTransaction("test", 100.toAmount(), true, "04/01/2024".toDate())

                transactionFeature.bookTransaction(tokenValue, booklet.label, transactionToSave)
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == user.id && it.id.accountId == booklet.id }
                    ?.transactions

                transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asNullableDomainResult()
                    .assertContainsAtPosition(3, transactionToSave)
            }
        }

        @Test
        fun `When I add a transaction with a same date as existing some, it should be in the last position of them`() {
            launchWithConnectedUserInstance() {
                initTransactions(mutableListOf(
                    generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate()),
                    generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("tes3", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("tes10", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate())
                ))
                val transactionToSave = generateTransaction("test", 100.toAmount(), true, "02/01/2024".toDate())
                transactionFeature.bookTransaction(tokenValue, booklet.label, transactionToSave)
                    .assertSuccess()
                val transactions = transactionState.getStates().find { it.id.userId == user.id && it.id.accountId == booklet.id }
                    ?.transactions

                transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asNullableDomainResult()
                    .assertContainsAtPosition(4, transactionToSave)
            }
        }
        @Test
        fun `Giving a user with a transaction, when booking a transaction that is older that the others, it should have its position to 0`() {
            launchWithConnectedUserInstance(

            ) {
                initTransactions( mutableListOf(
                    generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate()),
                    generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("tes3", 100.toAmount(), true, "02/01/2024".toDate()),
                    generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate())
                ))
                val transactionToSave = generateTransaction("test", 100.toAmount(), true, "23/12/2023".toDate())

                transactionFeature.bookTransaction(tokenValue, booklet.label, transactionToSave)
                    .assertSuccess()
            }
        }
    }

    @Nested
    inner class RetrieveTransactionsByMonthAndYearFeature {
        @Test
        fun `As a user with existing transactions, I should retrieve them ordering by date and position`() {
            launchWithConnectedUserInstance {
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
                val response = transactionFeature.retrieveTransactionsByMonthAndYear(tokenValue, Month.JANUARY, 2024, booklet.label)
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
            launchWithConnectedUserInstance {
                val elements = generateTransaction("test1", 100.toAmount(), true, "01/02/2024".toDate())
                initTransactions(elements.asSingleton())
                val expectedLabel = "test1.0"
                val expectedAmount = 105.toAmount()
                val expectedDate = "02/02/2024".toDate()
                transactionFeature.editTransaction(
                    booklet.id!!, elements.copy(label = "test1.0", amount = 105.toAmount(), date = "02/02/2024".toDate()), tokenValue
                )

                val actualTransaction = transactionState.getStates().find { it.id.userId == user.id && it.id.accountId == booklet.id }
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
            launchWithConnectedUserInstance {
                initTransactions(listOf(t1, t2, t3, t4, t5))

                transactionFeature.editTransaction(booklet.id!!, t5.copy(date = "31/12/2023".toDate()), tokenValue)
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == user.id && it.id.accountId == booklet.id }
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
            launchWithConnectedUserInstance {
                initTransactions(listOf(t1, t2, t3, t4, t5))
                transactionFeature.editTransaction(booklet.id!!, t1.copy(date = "02/01/2024".toDate()), tokenValue)
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == user.id && it.id.accountId == booklet.id }
                    ?.transactions
                assertEquals(5, transactions?.size)

                transactions.sortedByDate()
                    .assertEqualsAtPosition(0, t2.label) { label }
                    .assertEqualsAtPosition(1, t3.label) { label }
                    .assertEqualsAtPosition(2, t1.label) { label }
            }
        }

        @Test
        fun `Giving a user that save a transaction, when we edit it, the new amount of the account should take in count`() {
            launchWithConnectedUserInstance {
                val transaction = generateTransaction("test0", 100.toAmount(), true, "02/01/2024".toDate())
                initTransactions(listOf(
                    transaction
                ))
                val transaction2 = generateTransaction("test1", 100.toAmount(), true, "02/01/2024".toDate())
                transactionFeature.bookTransaction(tokenValue, booklet.label, transaction2)
                transactionFeature.editTransaction(booklet.id!!, transaction2.copy(amount = 105.toAmount()), tokenValue)
                    .assertSuccess()

                val actualAccount = accountState.getStates().find { it.userId == user.id }?.booklet?.find { it.id == booklet.id }

                assertEquals(205.toAmount(), actualAccount!!.amount)
            }
        }
        @Test
        fun `Giving a user with existing transaction, it should be retrieving it by its ID`() {
            launchWithConnectedUserInstance {
                val toInsert = generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate())
                initTransactions(toInsert.asSingleton())
                transactionFeature.findById(toInsert.id!!, tokenValue)
                    .assertTrue { this.label == toInsert.label && this.amount == toInsert.amount }
            }
        }
    }
    @Nested
    inner class BookingPreviewTransaction {
        @Test
        fun `booking a preview transaction should not change the real amount of an account`() {
            launchWithConnectedUserInstance {
                val transactionPreviewTest = Transaction(UUID.randomUUID(), "test#0", "01/01/2024".toDate(), 100.toAmount(), true, isPreview = true)
                transactionFeature.bookTransaction(tokenValue, booklet.label, transactionPreviewTest)
                    .assertSuccess()
                val actualAccount = accountState.getStates().find { it.userId == user.id }?.booklet?.find { it.id == booklet.id }
                val actualAmount = actualAccount?.amount ?: 10.toAmount().negate()

                org.junit.jupiter.api.assertAll(
                    { assertEquals(0.toAmount(), actualAmount) }
                )
            }
        }
    }
    @Nested
    inner class DeleteByIdFeature {
        @Test
        fun `Giving a user with existing transaction, when we delete it, the new amount of the account should take in count`() {
            launchWithConnectedUserInstance {
                val transaction = generateTransaction("test0", 100.toAmount(), true, "02/01/2024".toDate())
                val transaction2 = generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate())
                val transaction3 = generateTransaction("test3", 100.toAmount(), true, "02/01/2024".toDate())
                val transaction4 = generateTransaction("test4", 100.toAmount(), true, "02/01/2024".toDate())
                initTransactions(listOf(
                    transaction, transaction2, transaction3, transaction4
                ))
                transactionFeature.deleteSheetsByIds(booklet.id!!, listOf(
                    transaction.id!!, transaction2.id!!
                ), tokenValue)
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == user.id && it.id.accountId == booklet.id }
                    ?.transactions

                assertNull(transactions!!.find { it.label == "test0" })
                assertNull(transactions.find { it.label == "test2" })
            }
        }

        @Test
        fun `delete transaction with invalid account must return not found`() {
            launchWithConnectedUserInstance {
                transactionFeature.deleteSheetsByIds(UUID.randomUUID(), listOf(
                    UUID.randomUUID(), UUID.randomUUID()
                ), tokenValue)
                    .assertFailure(ResultState.BOOKLET_NOT_FOUND)
            }

        }
        @Test
        fun `delete preview regular transaction should exclude month for regular transaction`() {
            launchWithConnectedUserInstance {
                val regularTransactionId = fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId("regular-1")
                val previewTransaction = generateTransaction("test-preview", 100.toAmount(), true, "02/01/2024".toDate(), isPreview = true)
                    .copy(regularTransactionId = regularTransactionId)

                initTransactions(listOf(previewTransaction))

                transactionFeature.deleteSheetsByIds(booklet.id!!, listOf(previewTransaction.id!!), tokenValue)
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
            launchWithConnectedUserInstance {
                val transactionPreviewTest =
                    generateTransaction("test#0", 100.toAmount(), true, "01/01/2024".toDate(), isPreview = true)
                transactionState.init(
                    listOf(
                        IdUserAccountByTransaction(
                            IdUserAccount(user.id, booklet.id!!),
                            mutableListOf(transactionPreviewTest)
                        )
                    )
                )

                transactionFeature.confirmPreviewTransaction(
                    tokenValue,
                    booklet.id,
                    transactionPreviewTest.id!!,
                    null,
                    null
                )
                    .assertSuccess()
            }
        }
        @Test
        fun `Confirm preview transaction with invalid booklet id must resolve booklet not found`() {
            launchWithConnectedUserInstance {
                val transactionPreviewTest =
                    generateTransaction("test#0", 100.toAmount(), true, "01/01/2024".toDate(), isPreview = true)
                transactionState.init(
                    listOf(
                        IdUserAccountByTransaction(
                            IdUserAccount(user.id, booklet.id!!),
                            mutableListOf(transactionPreviewTest)
                        )
                    )
                )

                val result = transactionFeature.confirmPreviewTransaction(
                    tokenValue,
                    UUID.randomUUID(),
                    transactionPreviewTest.id!!,
                    null,
                    null
                )

                result.assertFailure(ResultState.BOOKLET_NOT_FOUND)
                assertEquals("domain.transaction.confirm.booklet_not_found", result.errorInfo?.key)
            }
        }
        @Test
        fun `confirm preview transaction with a transaction that is not found must return to not found`() {
            launchWithConnectedUserInstance {
                transactionFeature.confirmPreviewTransaction(
                    tokenValue,
                    booklet.id!!,
                    UUID.randomUUID(),
                    null,
                    null
                ).assertFailure(ResultState.BOOKLET_NOT_FOUND)
            }
        }

        @Test
        fun `confirm preview with new amount should update transaction and account amount`() {
            launchWithConnectedUserInstance {
                val transactionPreviewTest = generateTransaction("test#1", 100.toAmount(), true, "01/01/2024".toDate(), isPreview = true)
                transactionState.init(
                    listOf(
                        IdUserAccountByTransaction(
                            IdUserAccount(user.id, booklet.id!!),
                            mutableListOf(transactionPreviewTest)
                        )
                    )
                )

                val newAmount = 150.toAmount()

                transactionFeature.confirmPreviewTransaction(
                    tokenValue,
                    booklet.id,
                    transactionPreviewTest.id!!,
                    newAmount,
                    null
                ).assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == user.id && it.id.accountId == booklet.id }
                    ?.transactions
                val updated = transactions?.find { it.id == transactionPreviewTest.id }
                assertNotNull(updated)
                assertFalse(updated!!.isPreview)
                assertEquals(newAmount, updated.amount)

                val actualAccount = accountState.getStates().find { it.userId == user.id }?.booklet?.find { it.id == booklet.id }
                assertEquals(newAmount, actualAccount?.amount)
            }
        }

        @Test
        fun `confirm preview with new date should update transaction date`() {
            launchWithConnectedUserInstance {
                val transactionPreviewTest = generateTransaction("test#2", 100.toAmount(), true, "01/01/2024".toDate(), isPreview = true)
                transactionState.init(
                    listOf(
                        IdUserAccountByTransaction(
                            IdUserAccount(user.id, booklet.id!!),
                            mutableListOf(transactionPreviewTest)
                        )
                    )
                )

                val newDate = "12/01/2024".toDate()

                transactionFeature.confirmPreviewTransaction(
                    tokenValue,
                    booklet.id,
                    transactionPreviewTest.id!!,
                    null,
                    newDate
                ).assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == user.id && it.id.accountId == booklet.id }
                    ?.transactions
                val updated = transactions?.find { it.id == transactionPreviewTest.id }
                assertNotNull(updated)
                assertFalse(updated!!.isPreview)
                assertEquals(newDate, updated.date)
            }
        }
    }
}

fun String.toDate(): LocalDate = LocalDate.parse(this, DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun MutableList<Transaction>?.sortedByDate(): Result<List<Transaction>> {
    return this?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
        .asNullableDomainResult()
}