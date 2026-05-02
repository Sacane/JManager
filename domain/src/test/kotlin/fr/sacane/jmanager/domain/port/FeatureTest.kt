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
import org.junit.jupiter.api.AfterEach
import java.time.LocalDate
import java.util.*

open class FeatureTest {

    private val bookletState: State<BookletsByOwner> = FakeFactory.bookletState()
    private val transactionState: State<IdBookletByTransaction> = FakeFactory.fakeTransactionRepository()
    private val userState: State<UserWithPassword> = FakeFactory.fakeUserRepository()
    private val tagState: BiState<UserTag, List<Tag>> = FakeFactory.tagTestState()

    @AfterEach
    fun cleanUp() {
        userState.clear()
        transactionState.clear()
        bookletState.clear()
    }
    companion object {
        private val tagRepository = FakeFactory.fakeTagRepository()
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

    fun launchWithUserId(action: UserIdBooklet.() -> Unit) {
        val userId = UserId(UUID.randomUUID())
        val user = User(userId, "John", null)
        userState.init(listOf(UserWithPassword(user, "test")))
        val booklet = createBooklet(user, "test", Amount(0))
        action(UserIdBooklet(userId, booklet))
    }

    fun launchWithNewUserId(action: UserIdOnly.() -> Unit) {
        val userId = UserId(UUID.randomUUID())
        userState.init(listOf(UserWithPassword(User(userId, "John", null), "test")))
        action(UserIdOnly(userId))
    }

    inner class UserIdOnly(val userId: UserId)

    inner class UserIdBooklet(
        val userId: UserId,
        val booklet: Booklet
    ) {
        val user: MinimalUserRepresentation = MinimalUserRepresentation(userId, "John")

        fun initTransactions(transactions: List<Transaction>) {
            transactionState.init(listOf(IdBookletByTransaction(IdUserBooklet(userId, booklet.id!!), transactions.toMutableList())))
        }
        fun initTags(tags: List<UserTag>) {
            tags.forEach {
                tagState.init(it)
            }
        }
    }
}