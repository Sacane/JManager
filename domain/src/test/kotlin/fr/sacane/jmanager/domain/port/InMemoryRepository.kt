package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.AccountRepository
import fr.sacane.jmanager.domain.port.spi.TransactionRepositoryPort
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.util.Random

class InMemoryTransactionRepository(
    private val inMemoryUserProvider: InMemoryUserProvider
): TransactionRepositoryPort {

    private val transactions = mutableSetOf<Transaction>()


    override fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction? {
        inMemoryUserProvider.users[userId]?.user
            ?.accounts
            ?.find { it.label == accountLabel }
        ?.transactions
        ?.add(transaction) ?: return null
        return transaction
    }


    override fun findAccountByLabel(userId: UserId, labelAccount: String): Account? {
        return inMemoryUserProvider.users[userId]?.user?.accounts?.find { it.label == labelAccount }
    }

    override fun findAccountById(accountId: Long): Account? {
        for(user in inMemoryUserProvider.users.values.map { it.user }) {
            for(account in user.accounts) {
                if(account.id == accountId) {
                    return account
                }
            }
        }
        return null
    }

    override fun saveAllSheets(transactions: List<Transaction>) {
        this.transactions.addAll(transactions)
    }

    override fun deleteAllSheetsById(sheetIds: List<Long>) {
        this.transactions.removeAll { it.id in sheetIds }
    }

    override fun findSheetByID(sheetID: Long): Transaction? {
        return this.transactions.find { it.id == sheetID }
    }

    override fun save(transaction: Transaction): Transaction? {
        return if(this.transactions.add(transaction)) transaction else null
    }


    override fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Account? {
        return inMemoryUserProvider.users[userId]?.user?.accounts?.find { it.label == label }
    }
}

class InMemoryUserRepository (
    private val inMemoryUserProvider: InMemoryUserProvider
): UserRepository {
    private val random = Random()

    override fun findUserById(userId: UserId): User? {
        return inMemoryUserProvider.users[userId]?.user
    }

    override fun findUserByIdWithAccounts(userId: UserId): User? {
        return inMemoryUserProvider.users[userId]?.user
    }

    override fun findByPseudonym(pseudonym: String): User? {
        return inMemoryUserProvider.users.values.find { it.user.username == pseudonym }?.user
    }

    override fun findByPseudonymWithEncodedPassword(pseudonym: String): UserWithPassword? {
        return inMemoryUserProvider.users.values.find { it.user.username == pseudonym }
    }

    override fun create(user: UserWithPassword): User? {
        if(inMemoryUserProvider.users.put(user.user.id, UserWithPassword(user.user, user.password)) == null) return null
        return user.user
    }

    override fun register(username: String, email: String, password: Password): User? {
        val element = User(id = UserId(random.nextLong()), username = username, email = email)
        inMemoryUserProvider.users[element.id] = UserWithPassword(element, password)
        return element
    }

    override fun upsert(user: User): User? {
        inMemoryUserProvider.users[user.id]?.password?.let {
            inMemoryUserProvider.users[user.id] = UserWithPassword(user, it)
        } ?: return null
        return user
    }
}

data class AccountByOwner(
    val account: List<Account>,
    val userId: UserId
)

class InMemoryAccountRepository(
    private val inMemoryUserProvider: InMemoryUserProvider
): AccountRepository, State<AccountByOwner> {

    override fun editFromAnother(account: Account): Account {
        inMemoryUserProvider.upsert(account)
        return account
    }

    override fun getLastSheetPosition(accountId: Long): Int {
        return 0
    }

    override fun save(ownerId: UserId, account: Account): Account? {
        inMemoryUserProvider.addAccount(ownerId, account)
        return account
    }

    override fun findAccountByIdWithTransactions(accountId: Long): Account? {
        return inMemoryUserProvider.findAccountById(accountId)
    }

    override fun deleteAccountById(accountId: Long) {
        inMemoryUserProvider.removeAccountById(accountId)
    }

    override fun upsert(account: Account): Account {
        account.id?.let {
            inMemoryUserProvider.upsert(account)
        }
        return account
    }

    override fun getStates(): Collection<AccountByOwner> {
        return inMemoryUserProvider.accountsByOwner()
    }

    override fun clear() {
        inMemoryUserProvider.clearAccounts()
    }

    override fun init(initialState: Collection<AccountByOwner>) {
        inMemoryUserProvider.initAccounts(initialState)
    }
}


class InMemoryUserProvider {
    val users = mutableMapOf<UserId, UserWithPassword>()
    val accounts = mutableMapOf<UserId, MutableList<Account>>()

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
        println("ACCOUNTS : ${accounts}")
        for(accountList in accounts.values) {
            val result = accountList.find { it.id == accountId }
            if(result != null) {
                println("TROUVE")
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
        println(accounts)
    }

}