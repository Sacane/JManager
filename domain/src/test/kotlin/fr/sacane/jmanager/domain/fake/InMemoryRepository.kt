package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.SubscriptionPlan
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.models.ConsentRecord
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.time.LocalDateTime
import java.time.Month
import java.util.*

data class IdUserBooklet(
    val userId: UserId,
    val bookletId: UUID
)

data class IdBookletByTransaction(
    val id: IdUserBooklet,
    val transactions: MutableList<Transaction>
)

class InMemoryTransactionRepository(
    private val inMemoryDatabase: InMemoryDatabase
): TransactionRepository, State<IdBookletByTransaction> {


    override fun persist(userId: UserId, bookletLabel: String, transaction: Transaction): Transaction? {
        val booklet = inMemoryDatabase.bookletsByOwner().find { it.userId == userId }?.booklets?.find { it.label == bookletLabel } ?: return null
        val userBookletId = booklet.id?.let { IdUserBooklet(userId, it) } ?: return null
        inMemoryDatabase.addTransaction(userBookletId, transaction)

        return transaction
    }

    override fun deleteAllTransactionsById(transactionIds: List<UUID>) {
        inMemoryDatabase.removeAllTransactionsById(transactionIds)
    }

    override fun findTransactionById(transactionId: UUID): Transaction? {
        return inMemoryDatabase.findTransactionById(transactionId)
    }

    override fun save(bookletId: UUID, transaction: Transaction): Transaction {
        val transactionWithId = if (transaction.id == null) {
            transaction.copy(id = UUID.randomUUID())
        } else {
            transaction
        }
        inMemoryDatabase.saveTransaction(bookletId, transactionWithId)
        return transactionWithId
    }

    override fun findBookletByLabelWithTransactions(label: String, userId: UserId): Booklet? {
        return inMemoryDatabase.findBookletByOwnerAndLabel(userId, label)
    }

    override fun findBookletByIdWithTransactions(id: UUID): Booklet? {
        return inMemoryDatabase.findBookletById(id)
    }

    override fun findTransactionsByBookletId(bookletId: UUID): List<Transaction>? {
        return inMemoryDatabase.findBookletById(bookletId)?.transactions
    }

    override fun findTransactionsByBookletYearAndMonth(
        bookletId: UUID,
        year: Int,
        month: Month
    ): List<Transaction>? {
        return inMemoryDatabase.findBookletById(bookletId)?.retrieveTransactionsSortedByDate(month, year)
    }

    override fun isPersonalTagUsed(tagId: UUID): Boolean {
        return inMemoryDatabase.isPersonalTagUsed(tagId)
    }

    override fun replacePersonalTagByDefault(tagId: UUID, defaultTag: Tag) {
        inMemoryDatabase.replacePersonalTagByDefault(tagId, defaultTag)
    }

    override fun getStates(): Collection<IdBookletByTransaction> {
        return inMemoryDatabase.findTransactions()
    }

    override fun clear() {
        inMemoryDatabase.clearTransactions()
    }

    override fun init(initialState: Collection<IdBookletByTransaction>) {
        inMemoryDatabase.addMassiveTransaction(initialState)
    }
}

class InMemoryUserRepository (
    private val inMemoryDatabase: InMemoryDatabase
): UserRepository, State<UserWithPassword> {
    private val random = Random()

    override fun findUserById(userId: UserId): User? {
        return inMemoryDatabase.users[userId]?.user
    }

    override fun findUserByIdWithBooklets(userId: UserId): User? {
        val user = inMemoryDatabase.users[userId]?.user ?: return null
        val booklets = inMemoryDatabase.bookletsByOwner().find { it.userId == userId }
        if(booklets != null) {
            for(booklet in booklets.booklets) {
                user.addBooklet(booklet)
            }
        }
        println(user)
        return user
    }

    override fun findByPseudonym(pseudonym: String): User? {
        return inMemoryDatabase.users.values.find { it.user.username == pseudonym }?.user
    }

    override fun findByPseudonymWithEncodedPassword(pseudonym: String): UserWithPassword? {
        return inMemoryDatabase.users.values.find { it.user.username == pseudonym }
    }

    override fun findByEmailWithEncodedPassword(email: String): UserWithPassword? {
        return inMemoryDatabase.users.values.find { it.user.email == email }
    }

    override fun create(user: UserWithPassword): User? {
        if(inMemoryDatabase.users.put(user.user.id, UserWithPassword(user.user, user.password)) == null) return null
        return user.user
    }

    override fun register(username: String, password: String, roles: Set<Role>, subscriptionPlan: SubscriptionPlan, email: String?, tosAcceptedAt: LocalDateTime?, tosVersion: String?, privacyAcceptedAt: LocalDateTime?, emailVerified: Boolean, mustChangePassword: Boolean): User {
        val consent = if (tosAcceptedAt != null && privacyAcceptedAt != null) {
            ConsentRecord(tosAcceptedAt, tosVersion, privacyAcceptedAt)
        } else null
        val element = User(id = UserId(UUID.randomUUID()), username = username, email = email, roles = roles, subscriptionPlan = subscriptionPlan, consent = consent, emailVerified = emailVerified, mustChangePassword = mustChangePassword)
        inMemoryDatabase.users[element.id] = UserWithPassword(element, password, roles)
        return element
    }

    override fun markEmailVerified(userId: UserId): Result<Unit> {
        val userWithPassword = inMemoryDatabase.users[userId] ?: return Result(ResultState.USER_NOT_FOUND)
        userWithPassword.user.emailVerified = true
        return success(Unit)
    }

    override fun upsert(user: User): User? {
        inMemoryDatabase.users[user.id]?.password?.let {
            inMemoryDatabase.users[user.id] = UserWithPassword(user, it)
        } ?: return null
        return user
    }

    override fun updateProjectionWindowDays(userId: UserId, projectionWindowDays: Int): Boolean {
        val userWithPassword = inMemoryDatabase.users[userId] ?: return false
        userWithPassword.user.updateProjectionWindowDays(projectionWindowDays)
        return true
    }

    override fun findAll(): List<User> {
        return inMemoryDatabase.users.values.map { it.user }
    }

    override fun deleteById(userId: UserId): Result<Unit> {
        if (!inMemoryDatabase.users.containsKey(userId)) return Result(ResultState.USER_NOT_FOUND)
        inMemoryDatabase.users.remove(userId)
        return success(Unit)
    }

    override fun recordConsent(userId: UserId, consent: ConsentRecord): Result<Unit> {
        val userWithPassword = inMemoryDatabase.users[userId] ?: return Result(ResultState.USER_NOT_FOUND)
        userWithPassword.user.consent = consent
        return success(Unit)
    }

    override fun findByIdWithEncodedPassword(userId: UserId): UserWithPassword? {
        return inMemoryDatabase.users[userId]
    }

    override fun updatePassword(userId: UserId, hashedPassword: String, clearMustChange: Boolean): Result<Unit> {
        val entry = inMemoryDatabase.users[userId] ?: return Result(ResultState.USER_NOT_FOUND)
        if (clearMustChange) entry.user.mustChangePassword = false
        inMemoryDatabase.users[userId] = UserWithPassword(entry.user, hashedPassword, entry.roles)
        return success(Unit)
    }

    override fun getStates(): Collection<UserWithPassword> {
        return inMemoryDatabase.users.values
    }

    override fun init(initialState: Collection<UserWithPassword>) {
        inMemoryDatabase.initUsers(initialState)
    }

    override fun clear() {
        inMemoryDatabase.clearUsers()
    }
}

data class BookletsByOwner(
    val booklets: List<Booklet>,
    val userId: UserId
) {
    fun existsById(bookletId: UUID): Booklet? {
        return booklets.find { it.id == bookletId }
    }
}

class InMemoryBookletRepository(
    private val inMemoryDatabase: InMemoryDatabase
): BookletRepository, State<BookletsByOwner> {

    override fun editFromAnother(booklet: Booklet): Booklet {
        inMemoryDatabase.upsert(booklet)
        return booklet
    }
    override fun save(ownerId: UserId, booklet: Booklet): Booklet {
        inMemoryDatabase.addBooklet(ownerId, booklet)
        return booklet
    }

    override fun findBookletByIdWithTransactions(bookletId: UUID): Booklet? {
        return inMemoryDatabase.findBookletById(bookletId)
    }

    override fun findBookletByLabelWithTransactions(userId: UserId, bookletLabel: String): Booklet? {
        return inMemoryDatabase.findBookletByOwnerAndLabel(userId, bookletLabel)
    }

    override fun deleteBookletById(bookletId: UUID) {
        inMemoryDatabase.removeBookletById(bookletId)
    }

    override fun upsert(booklet: Booklet): Booklet {
        booklet.id?.let {
            inMemoryDatabase.upsert(booklet)
        }
        return booklet
    }

    override fun update(booklet: Booklet) {
        upsert(booklet)
    }

    override fun updateMonthlyPeriodStartDay(bookletId: UUID, monthlyPeriodStartDay: Int, monthlyPeriodEndDay: Int?): Boolean {
        val booklet = inMemoryDatabase.findBookletById(bookletId) ?: return false
        booklet.updateMonthlyPeriodConfiguration(monthlyPeriodStartDay, monthlyPeriodEndDay)
        upsert(booklet)
        return true
    }

    override fun findBookletsForUser(userId: UserId): List<Booklet> {
        return inMemoryDatabase.bookletsByOwner().find { it.userId == userId }?.booklets ?: emptyList()
    }

    override fun getStates(): Collection<BookletsByOwner> {
        return inMemoryDatabase.bookletsByOwner()
    }

    override fun clear() {
        inMemoryDatabase.clearBooklets()
    }

    override fun init(initialState: Collection<BookletsByOwner>) {
        inMemoryDatabase.initBooklets(initialState)
    }
}

