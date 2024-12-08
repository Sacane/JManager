package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.*
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import kotlin.random.Random

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
                transactionFeature.bookTransaction(userId, session.tokenValue, account.label, transactionToSave)
                    .assertTrue {
                        this.transaction.amount == transactionToSave.amount && this.transaction.label == transactionToSave.label
                    }
                transactionFeature.bookTransaction(userId, session.tokenValue, account.label, transactionToSave2)
                    .assertTrue {
                        this.transaction.amount == transactionToSave2.amount && this.transaction.label == transactionToSave2.label
                    }

                val accountStates = accountState.getStates()
                val accountByOwnerTarget = accountStates.find { it.userId == userId }
                val accountExpected = accountByOwnerTarget?.account?.find { it.id == account.id }
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
                println("1")
                transactionFeature.bookTransaction(userId, tokenValue, account.label, toInsertAtFirst).assertSuccess()
                println("2")
                transactionFeature.bookTransaction(userId, tokenValue, account.label, toInsertAtMiddle).assertSuccess()
                println("3")
                transactionFeature.bookTransaction(userId, tokenValue, account.label, toInsertAtLast).assertSuccess()

                val state = transactionState.getStates()
                val transactions = state.find { it.id.userId == userId && it.id.accountId == account.id }
                    ?.transactions

                transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asResponse()
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

                transactionFeature.bookTransaction(userId, tokenValue, account.label, transactionToSave)
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.accountId == account.id }
                    ?.transactions

                transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asResponse()
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
                transactionFeature.bookTransaction(userId, tokenValue, account.label, transactionToSave)
                    .assertSuccess()
                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.accountId == account.id }
                    ?.transactions

                transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asResponse()
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

                transactionFeature.bookTransaction(userId, session.tokenValue, account.label, transactionToSave)
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
                val response = transactionFeature.retrieveTransactionsByMonthAndYear(userId = userId , session.tokenValue, Month.JANUARY, 2024, account.label)
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
                    userId.id!!, account.id!!, elements.copy(label = "test1.0", _amount = 105.toAmount(), date = "02/02/2024".toDate()), tokenValue
                )

                val actualTransaction = transactionState.getStates().find { it.id.userId == userId && it.id.accountId == account.id }
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
            val t3 = generateTransaction("tes3", 100.toAmount(), true, "02/01/2024".toDate())
            val t4 = generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate())
            val t5 = generateTransaction("test5", 100.toAmount(), true, "03/01/2024".toDate())
            launchWithConnectedUserInstance {
                initTransactions(listOf(t1, t2, t3, t4, t5))

                transactionFeature.editTransaction(userId.id!!, account.id!!, t5.copy(date = "31/12/2023".toDate()), session.tokenValue)
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.accountId == account.id }
                    ?.transactions
                transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asResponse()
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
                transactionFeature.editTransaction(userId.id!!, account.id!!, t1.copy(date = "02/01/2024".toDate()), session.tokenValue)
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.accountId == account.id }
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
                transactionFeature.bookTransaction(userId, tokenValue, account.label, transaction2)
                transactionFeature.editTransaction(userId.id!!, account.id!!, transaction2.copy(_amount = 105.toAmount()), session.tokenValue)
                    .assertSuccess()

                val actualAccount = accountState.getStates().find { it.userId == userId }?.account?.find { it.id == account.id }

                assertEquals(205.toAmount(), actualAccount!!.amount)
            }
        }
        @Test
        fun `Giving a user with existing transaction, it should be retrieving it by its ID`() {
            launchWithConnectedUserInstance {
                val toInsert = generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate())
                initTransactions(toInsert.asSingleton())
                transactionFeature.findById(userId.id!!, toInsert.id!!, tokenValue)
                    .assertTrue { this.label == toInsert.label && this.amount == toInsert.amount }
            }
        }
    }
    @Nested
    inner class BookingPreviewTransaction {
        @Test
        fun `booking a preview transaction should not change the real amount of an account`() {
            launchWithConnectedUserInstance {
                val transactionPreviewTest = Transaction(Random.nextLong(), "test#0", "01/01/2024".toDate(), 100.toAmount(), true, isPreview = true)
                transactionFeature.bookTransaction(userId, session.tokenValue, account.label, transactionPreviewTest)
                    .assertSuccess()
                val actualAccount = accountState.getStates().find { it.userId == userId }?.account?.find { it.id == account.id }
                val actualAmount = actualAccount?.amount ?: 10.toAmount().negate()
                val actualPreviewAmount = actualAccount?.previewAmount ?: 0.toAmount()

                org.junit.jupiter.api.assertAll(
                    { assertEquals(0.toAmount(), actualAmount) },
                    { assertEquals(100.toAmount(), actualPreviewAmount) }
                )
            }
        }
    }
}

fun String.toDate(): LocalDate = LocalDate.parse(this, DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun MutableList<Transaction>?.sortedByDate(): Result<List<Transaction>> {
    return this?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
        .asResponse()
}