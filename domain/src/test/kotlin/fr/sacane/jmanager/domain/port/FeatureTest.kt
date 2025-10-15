package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.fake.AccountByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserAccount
import fr.sacane.jmanager.domain.fake.IdUserAccountByTransaction
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.SessionManager
import org.junit.jupiter.api.AfterEach
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

open class FeatureTest {

    private val accountState: State<AccountByOwner> = FakeFactory.accountState()
    private val transactionState: State<IdUserAccountByTransaction> = FakeFactory.fakeTransactionRepository()
    private val userState: State<UserWithPassword> = FakeFactory.fakeUserRepository()
    private val sessionManager: SessionManager = FakeFactory.sessionManager()

    @AfterEach
    fun cleanUp() {
        userState.clear()
        transactionState.clear()
        accountState.clear()
    }
    companion object {
        fun generateToken(userId: UserId, username: String): String {
            return "${userId.value}||${UUID.randomUUID()}||${Role.USER.name}||$username"
        }
        fun generateTransaction(label: String, amount: Amount, isIncome: Boolean, localDate: LocalDate = LocalDate.now(), isPreview: Boolean = false): Transaction {
            return Transaction(Random.nextLong(), label, localDate, amount, isIncome, isPreview = isPreview)
        }
    }
    fun createAccount(userId: User, label: String, amount: Amount): Booklet {
        val id = Random.nextLong()
        val booklet = Booklet(id = id, amount = amount, labelAccount = label, owner = userId)
        accountState.init(
            AccountByOwner(booklet.asSingleton(), userId.id).asSingleton()
        )
        return booklet
    }
    private fun createAndConnect(username: String): UserToken {
        val userId = UserId(Random.nextLong())
        val user = User(userId, username, "$username@test.fr")
        userState.init(listOf(UserWithPassword(user,"test")))
        val tokenValue = generateToken(user.id, user.username)
        sessionManager.addSession(userId, AccessToken(userId, username, tokenValue))
        return user.withToken(tokenValue)
    }
    fun launchWithConnectedUserInstance(action: AccountTokenUserId.() -> Unit){
        val john = createAndConnect("John")
        val account = createAccount(User(john.user.id, john.user.username, null), "test", Amount(0))
        val token = john.token
        action(AccountTokenUserId(john.user, token, account))
        sessionManager.removeSession(john.user.id, token)
    }

    fun launchWithConnectedUserWithoutAccount(action: TokenUserId.() -> Unit){
        val john = createAndConnect("John")
        val token = john.token
        action(TokenUserId(john.user.id, token))
        sessionManager.removeSession(john.user.id, token)
    }

    inner class AccountTokenUserId(
        val user: MinimalUserRepresentation,
        val tokenValue: String,
        val booklet: Booklet
    ) {
        fun initTransactions(transactions: List<Transaction>) {
            transactionState.init(listOf(IdUserAccountByTransaction(IdUserAccount(user.id, booklet.id!!), transactions.toMutableList())))
        }
    }
    inner class TokenUserId(
        val userId: UserId,
        val tokenValue: String
    )
}