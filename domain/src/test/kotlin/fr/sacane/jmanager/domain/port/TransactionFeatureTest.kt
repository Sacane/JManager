package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.assertEquals
import fr.sacane.jmanager.domain.assertTrue
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

data class AccountTokenUserId(
    val userId: UserId,
    val tokenValue: UUID,
    val account: Account
)

class TransactionFeatureTest {
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
        private fun withConnectedUserGivingTransactions(transactions: List<Transaction>, action: AccountTokenUserId.() -> Unit){
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
                        println(this.position)
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
    inner class RetrieveSheetsByMonthAndYearFeature {
        @Test
        fun `As a user with existing transactions, I should retrieve them ordering by date and position`() {
            val t1 = generateTransaction("test1", 100.toAmount(), true, "01/01/2024".toDate(), 0)
            val t2 = generateTransaction("test2", 100.toAmount(), true, "02/01/2024".toDate(), 1)
            val t3 = generateTransaction("tes3", 100.toAmount(), true, "02/01/2024".toDate(), 2)
            val t4 = generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate(), 3)
            val t5 = generateTransaction("test4", 100.toAmount(), true, "03/01/2024".toDate(), 4)
            withConnectedUserGivingTransactions(
                mutableListOf(
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
}

fun String.toDate(): LocalDate = LocalDate.parse(this, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
