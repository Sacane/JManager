package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.TransactionFeatureTest.Companion
import fr.sacane.jmanager.domain.port.api.SessionManager
import org.junit.jupiter.api.AfterEach
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
        val session: AccessToken = AccessToken(UUID.randomUUID())
    }
    fun createAccount(userId: UserId, label: String, amount: Amount): Account {
        val id = Random.nextLong()
        val account = Account(id = id, amount = amount, labelAccount = label)
        accountState.init(
            AccountByOwner(account.asSingleton(), userId).asSingleton()
        )
        return account
    }
    fun createAndConnect(username: String): UserId {
        val userId = UserId(Random.nextLong())
        userState.init(listOf(UserWithPassword(User(userId, username, "$username@test.fr"), Password("test"))))
        sessionManager.addSession(userId, session)
        return userId
    }
    fun launchWithConnectedUserInstance(action: AccountTokenUserId.() -> Unit){
        val johnId = createAndConnect("John")
        val account = createAccount(johnId, "test", Amount(0))

        action(AccountTokenUserId(johnId, session.tokenValue, account))
        sessionManager.removeSession(johnId, session.tokenValue)
    }

    inner class AccountTokenUserId(
        val userId: UserId,
        val tokenValue: UUID,
        val account: Account
    ) {
        fun initTransactions(transactions: List<Transaction>) {
            transactionState.init(listOf(IdUserAccountByTransaction(IdUserAccount(userId, account.id!!), transactions.toMutableList())))
        }
    }
}