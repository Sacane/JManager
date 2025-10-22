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

data class IdUserAccountByTransaction(
    val id: IdUserAccount,
    val transactions: MutableList<Transaction>
)

class InMemoryTransactionRepository(
    private val inMemoryDatabase: InMemoryDatabase
): TransactionRepository, State<IdUserAccountByTransaction> {


    override fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction? {
        val accounts = inMemoryDatabase.accountsByOwner().find { it.userId == userId }?.booklet?.find { it.label == accountLabel } ?: return null
        val userAccountId = accounts.id?.let { IdUserAccount(userId, it) } ?: return null
        inMemoryDatabase.addTransaction(userAccountId, transaction)

        return transaction
    }

    override fun deleteAllSheetsById(sheetIds: List<UUID>) {
        inMemoryDatabase.removeAllTransactionsById(sheetIds)
    }

    override fun findTransactionById(transactionId: UUID): Transaction? {
        return inMemoryDatabase.findTransactionById(transactionId)
    }

    override fun save(accountId: UUID, transaction: Transaction): Transaction {
        inMemoryDatabase.saveTransaction(accountId, transaction)
        return transaction
    }

    override fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Booklet? {
        return inMemoryDatabase.findAccountByOwnerAndLabel(userId, label)
    }

    override fun findAccountWithTransactionById(id: UUID): Booklet? {
        return inMemoryDatabase.findAccountById(id)
    }

    override fun findTransactionsByBookletId(bookletId: UUID): List<Transaction>? {
        return inMemoryDatabase.findAccountById(bookletId)?.transactions
    }

    override fun findTransactionsByBookletYearAndMonth(
        bookletId: UUID,
        year: Int,
        month: Month
    ): List<Transaction>? {
        return inMemoryDatabase.findAccountById(bookletId)?.retrieveSheetSurroundAndSortedByDate(month, year)
    }

    override fun getStates(): Collection<IdUserAccountByTransaction> {
        return inMemoryDatabase.findTransactions()
    }

    override fun clear() {
        inMemoryDatabase.clearTransactions()
    }

    override fun init(initialState: Collection<IdUserAccountByTransaction>) {
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

    override fun findUserByIdWithAccounts(userId: UserId): User? {
        val user = inMemoryDatabase.users[userId]?.user ?: return null
        val accounts = inMemoryDatabase.accountsByOwner().find { it.userId == userId }
        if(accounts != null) {
            for(account in accounts.booklet) {
                user.addAccount(account)
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

data class AccountByOwner(
    val booklet: List<Booklet>,
    val userId: UserId
) {
    fun existsById(accountId: UUID): Booklet? {
        return booklet.find { it.id == accountId }
    }
}

class InMemoryBookletRepository(
    private val inMemoryDatabase: InMemoryDatabase
): BookletRepository, State<AccountByOwner> {

    override fun editFromAnother(booklet: Booklet): Booklet {
        inMemoryDatabase.upsert(booklet)
        return booklet
    }
    override fun save(ownerId: UserId, booklet: Booklet): Booklet {
        inMemoryDatabase.addAccount(ownerId, booklet)
        return booklet
    }

    override fun findAccountByIdWithTransactions(accountId: UUID): Booklet? {
        return inMemoryDatabase.findAccountById(accountId)
    }

    override fun findAccountByLabelWithTransactions(userId: UserId, accountLabel: String): Booklet? {
        return inMemoryDatabase.findAccountByOwnerAndLabel(userId, accountLabel)
    }

    override fun deleteAccountById(accountId: UUID) {
        inMemoryDatabase.removeAccountById(accountId)
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

    override fun findBookletsForUser(userId: UserId): List<Booklet> {
        return inMemoryDatabase.accountsByOwner().find { it.userId == userId }?.booklet ?: emptyList()
    }

    override fun getStates(): Collection<AccountByOwner> {
        return inMemoryDatabase.accountsByOwner()
    }

    override fun clear() {
        inMemoryDatabase.clearAccounts()
    }

    override fun init(initialState: Collection<AccountByOwner>) {
        inMemoryDatabase.initAccounts(initialState)
    }
}

data class IdUserAccount(
    val userId: UserId,
    val accountId: UUID
)


