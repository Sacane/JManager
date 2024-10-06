package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.*
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random

fun <T> T.asSingleton(): List<T> = listOf(this)


class TransactionFeatureTest {
    data class AccountTokenUserId(
        val userId: UserId,
        val tokenValue: UUID,
        val account: Account
    )
    companion object{
        private val userState: State<UserWithPassword> = FakeFactory.fakeUserRepository()
        private val transactionState: State<IdUserAccountByTransaction> = FakeFactory.fakeTransactionRepository()
        private val accountState: State<AccountByOwner> = FakeFactory.accountState()

        private val transactionFeature = FakeFactory.transactionFeature

        private val tokenValue = UUID.randomUUID()
        private val session: AccessToken = AccessToken(tokenValue)

        private fun createAndConnect(username: String): UserId {
            val userId = UserId(Random.nextLong())
            userState.init(listOf(UserWithPassword(User(userId, username, "$username@test.fr"), Password("test"))))
            FakeFactory.sessionManager.addSession(userId, session)
            return userId
        }
        private fun withConnectedUserGivingTransactions(transactions: List<Transaction> = listOf(), action: AccountTokenUserId.() -> Unit){
            val johnId = createAndConnect("John")
            val account = createAccount(johnId, "test", Amount(100))
            val idUserAccount = IdUserAccount(johnId, account.id!!)

            transactionState.init(listOf(IdUserAccountByTransaction(idUserAccount, transactions.toMutableList())))

            action(AccountTokenUserId(johnId, session.tokenValue, account))
            FakeFactory.sessionManager.removeSession(johnId, session.tokenValue)
        }

        private fun createAccount(userId: UserId, label: String, amount: Amount): Account {
            val id = Random.nextLong()
            val account = Account(id = id, amount = amount, labelAccount = label)
            accountState.init(
                AccountByOwner(account.asSingleton(), userId).asSingleton()
            )
            return account
        }
        private fun generateTransaction(label: String, amount: Amount, isIncome: Boolean, localDate: LocalDate = LocalDate.now(), position: Int = 0): Transaction{
            return Transaction(Random.nextLong(), label, localDate, amount, isIncome, position = position)
        }
    }


    @AfterEach
    fun cleanUp() {
        userState.clear()
        transactionState.clear()
        accountState.clear()
    }

    @Nested
    inner class SaveTransactionInAccountFeatureTest {
        @Test
        fun `When I add a new transaction, it should persist it and update the account amount when its income and outcome`() {
            val johnId = createAndConnect("John")
            val account = createAccount(johnId, "test", Amount(100))
            val transactionToSave = generateTransaction("test", 100.toAmount(), true)
            val transactionToSave2 = generateTransaction("test", 50.toAmount(), false)

            transactionFeature.saveInAccount(johnId, session.tokenValue, account.label, transactionToSave)
                .assertTrue {
                    this.amount == transactionToSave.amount && this.label == transactionToSave.label
                }
            transactionFeature.saveInAccount(johnId, session.tokenValue, account.label, transactionToSave2)
                .assertTrue {
                    this.amount == transactionToSave2.amount && this.label == transactionToSave2.label
                }

            val accountStates = accountState.getStates()
            assertTrue(accountStates.contains(AccountByOwner(account.asSingleton(), johnId)))

            val accountByOwnerTarget = accountStates.find { it.userId == johnId }
            val accountExpected = accountByOwnerTarget?.account?.find { it.id == account.id }
            assertNotNull(accountExpected)
            assertEquals(Amount(150), accountExpected?.sold)
        }

        @Test
        fun `When I add a transaction in an account that already have some, its position should be coherent regarding the date`() {
            withConnectedUserGivingTransactions(listOf(
                generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate(), 0),
                generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate(), 1),
                generateTransaction("tes3", 100.toAmount(), true, "03/01/2024".toDate(), 2),
                generateTransaction("test4", 100.toAmount(), true, "04/01/2024".toDate(), 3)
            )){
                val transactionToSave = generateTransaction("test", 100.toAmount(), true, "05/01/2024".toDate())
                transactionFeature.saveInAccount(this.userId, this.tokenValue, this.account.label, transactionToSave)
                    .assertTrue {
                        this.amount == transactionToSave.amount
                                && this.label == transactionToSave.label
                                && this.position == 4
                    }
            }
        }

        @Test
        fun `When I add a transaction in the middle of an account that already have some, its position should be coherent regarding the date`() {
            withConnectedUserGivingTransactions(listOf(
                generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate(), 0),
                generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate(), 1),
                generateTransaction("tes3", 100.toAmount(), true, "03/01/2024".toDate(), 2),
                generateTransaction("test4", 100.toAmount(), true, "05/01/2024".toDate(), 3)
            )) {
                val transactionToSave = generateTransaction("test", 100.toAmount(), true, "04/01/2024".toDate())

                transactionFeature.saveInAccount(userId, tokenValue, account.label, transactionToSave)
                    .assertTrue {
                        this.amount == transactionToSave.amount
                                && this.label == transactionToSave.label
                                && this.position == 3
                    }

                val transactions = transactionState.getStates().find { it.id.userId == userId }
                assertNotNull(transactions)
                val expectedTransactionPosition = 4
                val actualTransactionPosition = transactions!!.transactions.find { it.label == "test4" }!!.position
                assertEquals(expectedTransactionPosition, actualTransactionPosition)
            }
        }

        @Test
        fun `When I add a transaction with a same date as existing some, it should be in the last position of them`() {
            withConnectedUserGivingTransactions(mutableListOf(
                generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate(), 0),
                generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate(), 1),
                generateTransaction("tes3", 100.toAmount(), true, "02/01/2024".toDate(), 2),
                generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate(), 3)
            )) {


                val transactionToSave = generateTransaction("test", 100.toAmount(), true, "02/01/2024".toDate())

                transactionFeature.saveInAccount(userId, session.tokenValue, account.label, transactionToSave)
                    .map { it.position }
                    .assertEquals(3)

                assertEquals(5,
                    transactionState.getStates()
                        .find { it.id == IdUserAccount(userId, account.id!!) }?.transactions?.size
                )
            }
        }
        @Test
        fun `Giving a user with a transaction, when booking a transaction that is older that the others, it should have its position to 0`() {
            withConnectedUserGivingTransactions(
                mutableListOf(
                    generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate(), 0),
                    generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate(), 1),
                    generateTransaction("tes3", 100.toAmount(), true, "02/01/2024".toDate(), 2),
                    generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate(), 3)
                )
            ) {
                val transactionToSave = generateTransaction("test", 100.toAmount(), true, "23/12/2023".toDate())

                transactionFeature.saveInAccount(userId, session.tokenValue, account.label, transactionToSave)
                    .map { it.position }
                    .assertEquals(0)
            }
        }
    }

    @Nested
    inner class RetrieveTransactionsByMonthAndYearFeature {
        @Test
        fun `As a user with existing transactions, I should retrieve them ordering by date and position`() {
            val t1 = generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate(), 0)
            val t2 = generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate(), 1)
            val t3 = generateTransaction("tes3", 100.toAmount(), true, "02/01/2024".toDate(), 2)
            val t4 = generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate(), 3)
            val t5 = generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate(), 4)
            withConnectedUserGivingTransactions(
                listOf(
                    t1, t2, t4, t3, t5,
                    generateTransaction("test5", 100.toAmount(), true, "01/02/2024".toDate(), 5),
                    generateTransaction("test6", 100.toAmount(), true, "01/02/2024".toDate(), 6),
                )
            ) {
                val response = transactionFeature.retrieveTransactionsByMonthAndYear(userId, session.tokenValue, Month.JANUARY, 2024, account.label)
                response.assertTrue {
                    size == 5 && all { it.date.month == Month.JANUARY }
                }
                response.assertEquals(listOf(t1, t2, t3, t4, t5))
            }
        }
    }
    @Nested
    inner class EditTransactionFeature {

        @Test
        fun `Giving an existing transaction, I should correctly edit label, amount and date from it`() {
            val elements = generateTransaction("test1", 100.toAmount(), true, "01/02/2024".toDate(), 0)
            withConnectedUserGivingTransactions(
                elements.asSingleton()
            ) {
                val expectedLabel = "test1.0"
                val expectedAmount = 105.toAmount()
                val expectedDate = "02/02/2024".toDate()
                transactionFeature.editTransaction(
                    userId.id!!, account.id!!, elements.copy(label = "test1.0", amount = 105.toAmount(), date = "02/02/2024".toDate()), tokenValue
                ).assertTrue { label == expectedLabel && amount == expectedAmount && date == expectedDate}

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
            withConnectedUserGivingTransactions(
                listOf(t1, t2, t3, t4, t5)
            ) {
                transactionFeature.editTransaction(userId.id!!, account.id!!, t5.copy(date = "31/12/2023".toDate()), session.tokenValue)
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.accountId == account.id }
                    ?.transactions

                transactions?.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asResponse()
                    .assertContainsAtPosition(0, t5)
                    .assertContainsAtPosition(1, t1)
                    .assertContainsAtPosition(2, t2)
                    .assertContainsAtPosition(3, t3)
                    .assertContainsAtPosition(4, t4)
            }
        }
        @Test
        fun `Giving existing transactions, when one is edited with a more recent new date, all the position after should still be coherent`() {
            val t1 = generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate())
            val t2 = generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate())
            val t3 = generateTransaction("test3", 100.toAmount(), true, "02/01/2024".toDate())
            val t4 = generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate())
            val t5 = generateTransaction("test5", 100.toAmount(), true, "04/01/2024".toDate())
            withConnectedUserGivingTransactions(
                listOf(t1, t2, t3, t4, t5)
            ) {
                transactionFeature.editTransaction(userId.id!!, account.id!!, t1.copy(date = "02/01/2024".toDate()), session.tokenValue)
                    .assertSuccess()

                val transactions = transactionState.getStates().find { it.id.userId == userId && it.id.accountId == account.id }
                    ?.transactions ?: fail()

                transactions.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.lastModified })
                    .asResponse()
                    .assertContainsAtPosition(0, t2)
                    .assertContainsAtPosition(1, t3)
                    .assertContainsAtPosition(2, t1)
            }
        }

        @Test
        fun `Giving existing transaction, `() {

        }
    }
}

fun String.toDate(): LocalDate = LocalDate.parse(this, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
