package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.AccountRepository
import fr.sacane.jmanager.domain.port.spi.TransactionRepositoryPort
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.util.Random

class InMemoryTransactionRepository(
    private val inMemoryDatabase: InMemoryDatabase
): TransactionRepositoryPort {


    override fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction? {
        val accounts = inMemoryDatabase.accountsByOwner().find { it.userId == userId }?.account?.find { it.label == accountLabel } ?: return null
        val userAccountId = accounts.id?.let { IdUserAccount(userId, it) } ?: return null
        inMemoryDatabase.addTransaction(userAccountId, transaction)

        return transaction
    }


    override fun findAccountByLabel(userId: UserId, labelAccount: String): Account? {
        return inMemoryDatabase.users[userId]?.user?.accounts?.find { it.label == labelAccount }
    }

    override fun findAccountById(accountId: Long): Account? {
        for(user in inMemoryDatabase.users.values.map { it.user }) {
            for(account in user.accounts) {
                if(account.id == accountId) {
                    return account
                }
            }
        }
        return null
    }

    override fun saveAllSheets(transactions: List<Transaction>) {
        inMemoryDatabase.upsertTransactions(transactions)
    }

    override fun deleteAllSheetsById(sheetIds: List<Long>) {
        inMemoryDatabase.removeAllTransactionsById(sheetIds)
    }

    override fun findTransactionById(transactionId: Long): Transaction? {
        return inMemoryDatabase.findTransactionById(transactionId)
    }

    override fun save(transaction: Transaction): Transaction {
        saveAllSheets(listOf(transaction))
        return transaction
    }


    override fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Account? {
        return inMemoryDatabase.users[userId]?.user?.accounts?.find { it.label == label }
    }
}

class InMemoryUserRepository (
    private val inMemoryDatabase: InMemoryDatabase
): UserRepository {
    private val random = Random()

    override fun findUserById(userId: UserId): User? {
        return inMemoryDatabase.users[userId]?.user
    }

    override fun findUserByIdWithAccounts(userId: UserId): User? {
        return inMemoryDatabase.users[userId]?.user
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

    override fun register(username: String, email: String, password: Password): User {
        val element = User(id = UserId(random.nextLong()), username = username, email = email)
        inMemoryDatabase.users[element.id] = UserWithPassword(element, password)
        return element
    }

    override fun upsert(user: User): User? {
        inMemoryDatabase.users[user.id]?.password?.let {
            inMemoryDatabase.users[user.id] = UserWithPassword(user, it)
        } ?: return null
        return user
    }
}

data class AccountByOwner(
    val account: List<Account>,
    val userId: UserId
)

class InMemoryAccountRepository(
    private val inMemoryDatabase: InMemoryDatabase
): AccountRepository, State<AccountByOwner> {

    override fun editFromAnother(account: Account): Account {
        inMemoryDatabase.upsert(account)
        return account
    }

    override fun getLastSheetPosition(accountId: Long): Int {
        return 0
    }

    override fun save(ownerId: UserId, account: Account): Account? {
        inMemoryDatabase.addAccount(ownerId, account)
        return account
    }

    override fun findAccountByIdWithTransactions(accountId: Long): Account? {
        return inMemoryDatabase.findAccountById(accountId)
    }

    override fun deleteAccountById(accountId: Long) {
        inMemoryDatabase.removeAccountById(accountId)
    }

    override fun upsert(account: Account): Account {
        account.id?.let {
            inMemoryDatabase.upsert(account)
        }
        return account
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
    val accountId: Long
)

class InMemoryDatabase {
    val users = mutableMapOf<UserId, UserWithPassword>()
    private val accounts = mutableMapOf<UserId, MutableList<Account>>()
    private val transactions = mutableMapOf<IdUserAccount, MutableList<Transaction>>()

    fun addAccount(ownerId: UserId, account: Account) {
        accounts.computeIfAbsent(ownerId) { mutableListOf() }.add(account)
    }
    fun removeAccountById(accountId: Long) {
        accounts.forEach { (key, value) ->
            value.removeIf { it.id == accountId }
        }
    }

    fun upsert(account: Account) {
        val accountId = account.id
        var ownerId = UserId(0)
        accounts.forEach { (key, value) ->
            value.removeIf { it.id == accountId }
            ownerId = key
        }
        accounts.computeIfAbsent(ownerId) { mutableListOf() }.add(account)
    }

    fun findAccountById(accountId: Long): Account? {
        for(accountList in accounts.values) {
            val result = accountList.find { it.id == accountId }
            if(result != null) {
                return result
            }
        }
        return null
    }

    fun clearAccounts() {
        accounts.clear()
    }

    fun accountsByOwner(): Collection<AccountByOwner> {
        return accounts.map { AccountByOwner(it.value, it.key) }
    }

    fun initAccounts(initialState: Collection<AccountByOwner>) {
        initialState.forEach {
            accounts[it.userId] = it.account.toMutableList()
        }
    }

    fun addTransaction(userAccountId: IdUserAccount, transaction: Transaction) {
         transactions.computeIfAbsent(userAccountId) { mutableListOf() }.add(transaction)
    }

    fun upsertTransactions(transactionList: List<Transaction>) {
        transactions.forEach { (key, trs) ->
            trs.removeIf { transaction -> transaction.id in transactionList.map { it.id } }
            trs.addAll(transactionList)
        }
    }
    fun removeAllTransactionsById(transactionIds: List<Long>) {
        transactions.forEach { (key, trs) ->
            trs.removeIf { it.id in transactionIds }
        }
    }

    fun findTransactionById(transactionId: Long): Transaction? {
        transactions.forEach {
            return it.value.find { tr -> tr.id == transactionId }
        }
        return null
    }
}