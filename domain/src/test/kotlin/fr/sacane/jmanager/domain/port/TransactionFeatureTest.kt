package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.toAmount
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

fun <T> T.asSingleton(): List<T> = listOf(this)

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

        private fun createAccount(userId: UserId, label: String, amount: Amount): Account {
            val id = Random.nextLong()
            val account = Account(id = id, amount = amount, labelAccount = label)
            accountState.init(
                AccountByOwner(account.asSingleton(), userId).asSingleton()
            )
            return account
        }
        private fun generateTransaction(label: String, amount: Amount, isIncome: Boolean, localDate: LocalDate = LocalDate.now()): Transaction{
            return Transaction(Random.nextLong(), label, localDate, amount, isIncome)
        }
    }



    @AfterEach
    fun cleanUp() {
        userState.clear()
        transactionState.clear()
        accountState.clear()
    }

    @Test
    fun `As an account owner, when I add a new transaction, it should persist it and update the account amount`() {
        val userId = createAndConnect("John")
        val account = createAccount(userId, "test", Amount(100))

        val transactionToSave = generateTransaction("test", 100L.toAmount(), true)
        transactionFeature.saveAndLink(userId, session.tokenValue, account.label, transactionToSave)
            .assertTrue {
                this.amount == transactionToSave.amount && this.label == transactionToSave.label
            }

    }
}