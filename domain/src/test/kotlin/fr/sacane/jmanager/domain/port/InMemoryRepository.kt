package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.AccountRepository
import fr.sacane.jmanager.domain.port.spi.TransactionRepositoryPort
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.util.Random

data class IdUserAccountByTransaction(
    val id: IdUserAccount,
    val transactions: MutableList<Transaction>
)

class InMemoryTransactionRepository(
    private val inMemoryDatabase: InMemoryDatabase
): TransactionRepositoryPort, State<IdUserAccountByTransaction> {


    override fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction? {
        val accounts = inMemoryDatabase.accountsByOwner().find { it.userId == userId }?.account?.find { it.label == accountLabel } ?: return null
        val userAccountId = accounts.id?.let { IdUserAccount(userId, it) } ?: return null
        inMemoryDatabase.addTransaction(userAccountId, transaction)

        return transaction
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
        return inMemoryDatabase.findAccountByOwnerAndLabel(userId, label)
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
            for(account in accounts.account) {
                user.addAccount(account)
            }
        }
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
    val account: List<Account>,
    val userId: UserId
) {
    fun existsById(accountId: Long): Account? {
        return account.find { it.id == accountId }
    }
}

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

    override fun findAccountByLabelWithTransactions(userId: UserId, accountLabel: String): Account? {
        return inMemoryDatabase.findAccountByOwnerAndLabel(userId, accountLabel)
    }

    override fun deleteAccountById(accountId: Long) {
        inMemoryDatabase.removeAccountById(accountId)
    }

    override fun upsert(account: Account): Account {
        account.id?.let {
            inMemoryDatabase.upsert(account)
            inMemoryDatabase.upsertTransactions(account.transactions)
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
    private val transactions = mutableMapOf<IdUserAccount, IdUserAccountByTransaction>()

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
            value.removeAll { it.id == accountId }
            ownerId = key
        }
        accounts.computeIfAbsent(ownerId) { mutableListOf() }.add(account)
    }

    fun findAccountById(accountId: Long): Account? {
        for(accountList in accounts.values) {
            val result = accountList.find { it.id == accountId }
            val accountCopy = Account(result!!.id, result.amount, result.label, result.transactions, result.owner, result.initialSold)
            for(transaction in transactions) {
                if(transaction.key.accountId == accountId) {
                    accountCopy.addAllTransaction(transaction.value.transactions)
                    break
                }
            }
            return accountCopy.also { println("#0 ${it.amount}") }
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
        initialState.forEach { accByOwn ->
            accByOwn.account.forEach {
                addAccount(accByOwn.userId, it)
            }
        }
    }

    fun addTransaction(userAccountId: IdUserAccount, transaction: Transaction) {
        transactions.computeIfAbsent(userAccountId) { IdUserAccountByTransaction(userAccountId, mutableListOf(transaction)) }.transactions.add(transaction)
    }
    fun addMassiveTransaction(collection: Collection<IdUserAccountByTransaction>){
        collection.forEach { idByTr ->
            println(collection)
            accounts.computeIfAbsent(idByTr.id.userId) { mutableListOf() }.find { it.id == idByTr.id.accountId }?.addAllTransaction(idByTr.transactions)
            transactions.computeIfAbsent(idByTr.id) { IdUserAccountByTransaction(idByTr.id, idByTr.transactions) }
        }
    }

    fun upsertTransactions(transactionList: List<Transaction>) {
        transactions.forEach { (key, trs) ->
            trs.transactions.removeAll { transaction -> transaction.id in transactionList.map { it.id } }
            trs.transactions.addAll(transactionList)

        }
    }
    fun removeAllTransactionsById(transactionIds: List<Long>) {
        transactions.forEach { (key, trs) ->
            trs.transactions.removeIf { it.id in transactionIds }
        }
    }

    fun findTransactionById(transactionId: Long): Transaction? {
        transactions.forEach {
            return it.value.transactions.find { tr -> tr.id == transactionId }
        }
        return null
    }

    fun clearUsers() {
        users.clear()
    }

    fun initUsers(userCollection: Collection<UserWithPassword>) {
        users.putAll(userCollection.associateBy { it.user.id })
    }

    fun findTransactions(): Collection<IdUserAccountByTransaction> {
        return transactions.values
    }

    fun findAccountByOwnerAndLabel(userId: UserId, accountLabel: String): Account? {
        accounts.entries.filter{it.key == userId}.forEach { accByOwn ->
            val acc = accByOwn.value.find { acc -> acc.label == accountLabel } ?: return null
            val copyAcc = Account(acc.id, acc.amount, acc.label, acc.transactions, acc.owner, acc.initialSold)
            transactions.forEach {
                if(it.key.accountId == acc.id) {
                    copyAcc.addAllTransaction(it.value.transactions)
                }
            }
            return copyAcc
        }
        return null
    }

    fun clearTransactions() {
        transactions.clear()
    }
}
