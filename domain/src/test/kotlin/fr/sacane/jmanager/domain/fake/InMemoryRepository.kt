package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.spi.UserRepository
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

    override fun deleteAllSheetsById(sheetIds: List<UUID>) {
        inMemoryDatabase.removeAllTransactionsById(sheetIds)
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

    override fun findBookletByLabelWithSheets(label: String, userId: UserId): Booklet? {
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
        return inMemoryDatabase.findBookletById(bookletId)?.retrieveSheetSurroundAndSortedByDate(month, year)
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

    override fun create(user: UserWithPassword): User? {
        if(inMemoryDatabase.users.put(user.user.id, UserWithPassword(user.user, user.password)) == null) return null
        return user.user
    }

    override fun register(username: String, password: String, roles: Set<Role>): User {
        val element = User(id = UserId(UUID.randomUUID()), username = username, null, roles = roles)
        inMemoryDatabase.users[element.id] = UserWithPassword(element, password, roles)
        return element
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

