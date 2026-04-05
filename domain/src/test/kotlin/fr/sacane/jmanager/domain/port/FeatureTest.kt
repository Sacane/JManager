package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.BiState
import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.fake.BookletsByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserBooklet
import fr.sacane.jmanager.domain.fake.IdBookletByTransaction
import fr.sacane.jmanager.domain.fake.UserTag
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.SessionManager
import org.junit.jupiter.api.AfterEach
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

open class FeatureTest {

    private val bookletState: State<BookletsByOwner> = FakeFactory.bookletState()
    private val transactionState: State<IdBookletByTransaction> = FakeFactory.fakeTransactionRepository()
    private val userState: State<UserWithPassword> = FakeFactory.fakeUserRepository()
    private val sessionManager: SessionManager = FakeFactory.sessionManager()
    private val tagState: BiState<UserTag, List<Tag>> = FakeFactory.tagTestState()

    @AfterEach
    fun cleanUp() {
        userState.clear()
        transactionState.clear()
        bookletState.clear()
    }
    companion object {
        private val tagRepository = FakeFactory.fakeTagRepository()
        fun generateToken(userId: UserId, username: String): String {
            return "${userId.value}||${UUID.randomUUID()}||${Role.USER.name}||$username"
        }
        fun generateTransaction(label: String, amount: Amount, isIncome: Boolean, localDate: LocalDate = LocalDate.now(), tag: Tag? = null, isPreview: Boolean = false): Transaction {
            return Transaction(UUID.randomUUID(), label, localDate, amount, isIncome, isPreview = isPreview, tag = tag ?: tagRepository.defaultTag())
        }
    }
    fun createBooklet(userId: User, label: String, amount: Amount): Booklet {
        val id = UUID.randomUUID()
        val booklet = Booklet(id = id, amount = amount, label = label, owner = userId)
        bookletState.init(
            BookletsByOwner(booklet.asSingleton(), userId.id).asSingleton()
        )
        return booklet
    }
    private fun createAndConnect(username: String): UserToken {
        val userId = UserId(UUID.randomUUID())
        val user = User(userId, username, "$username@test.fr")
        userState.init(listOf(UserWithPassword(user,"test")))
        val tokenValue = generateToken(user.id, user.username)
        sessionManager.addSession(userId, AccessToken(userId, username, tokenValue))
        return user.withToken(tokenValue)
    }
    fun launchWithConnectedUserInstance(action: BookletTokenUserId.() -> Unit){
        val john = createAndConnect("John")
        val booklet = createBooklet(User(john.user.id, john.user.username, null), "test", Amount(0))
        val token = john.token
        action(BookletTokenUserId(john.user, token, booklet))
        sessionManager.removeSession(john.user.id, token)
    }

    fun launchWithConnectedUserWithoutBooklet(action: TokenUserId.() -> Unit){
        val john = createAndConnect("John")
        val token = john.token
        action(TokenUserId(john.user.id, token))
        sessionManager.removeSession(john.user.id, token)
    }

    inner class BookletTokenUserId(
        val user: MinimalUserRepresentation,
        val tokenValue: String,
        val booklet: Booklet
    ) {
        fun initTransactions(transactions: List<Transaction>) {
            transactionState.init(listOf(IdBookletByTransaction(IdUserBooklet(user.id, booklet.id!!), transactions.toMutableList())))
        }
        fun initTags(tags: List<UserTag>) {
            tags.forEach {
                tagState.init(it)
            }
        }
    }
    inner class TokenUserId(
        val userId: UserId,
        val tokenValue: String
    )
}