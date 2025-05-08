package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.fake.AccountByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserAccount
import fr.sacane.jmanager.domain.fake.IdUserAccountByTransaction
import fr.sacane.jmanager.domain.models.*
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
        fun generateToken(userId: UserId): String {
            return "${userId.value}||${UUID.randomUUID()}||${Role.USER.name}"
        }
        fun generateTransaction(label: String, amount: Amount, isIncome: Boolean, localDate: LocalDate = LocalDate.now(), isPreview: Boolean = false): Transaction{
            return Transaction(Random.nextLong(), label, localDate, amount, isIncome, isPreview = isPreview)
        }
    }
    fun createAccount(userId: User, label: String, amount: Amount): Account {
        val id = Random.nextLong()
        val account = Account(id = id, amount = amount, labelAccount = label, owner = userId)
        accountState.init(
            AccountByOwner(account.asSingleton(), userId.id).asSingleton()
        )
        return account
    }
    private fun createAndConnect(username: String): UserToken {
        val userId = UserId(Random.nextLong())
        val user = User(userId, username, "$username@test.fr")
        userState.init(listOf(UserWithPassword(user,"test")))
        val tokenValue = generateToken(user.id)
        sessionManager.addSession(userId, AccessToken(userId, tokenValue))
        return user.withToken(tokenValue)
    }
    fun launchWithConnectedUserInstance(action: AccountTokenUserId.() -> Unit){
        val john = createAndConnect("John")
        val account = createAccount(User(john.user.id, john.user.username, null), "test", Amount(0))
        val token = john.token
        action(AccountTokenUserId(john.user.id, token, account))
        sessionManager.removeSession(john.user.id, token)
    }

    inner class AccountTokenUserId(
        val userId: UserId,
        val tokenValue: String,
        val account: Account
    ) {
        fun initTransactions(transactions: List<Transaction>) {
            transactionState.init(listOf(IdUserAccountByTransaction(IdUserAccount(userId, account.id!!), transactions.toMutableList())))
        }
    }
}