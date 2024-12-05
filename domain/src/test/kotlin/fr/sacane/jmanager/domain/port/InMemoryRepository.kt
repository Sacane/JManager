package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.AccountRepositoryPort
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

    override fun save(accountId: Long, transaction: Transaction): Transaction {
        inMemoryDatabase.saveTransaction(accountId, transaction)
        return transaction
    }

    override fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Account? {
        return inMemoryDatabase.findAccountByOwnerAndLabel(userId, label).also { println(it?.transactions) }
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

    override fun register(username: String, password: String): User {
        val element = User(id = UserId(random.nextLong()), username = username, null)
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
): AccountRepositoryPort, State<AccountByOwner> {

    override fun editFromAnother(account: Account): Account {
        inMemoryDatabase.upsert(account)
        return account
    }
    override fun save(ownerId: UserId, account: Account): Account {
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
        }
        return account
    }

    override fun update(account: Account) {
        upsert(account)
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

    // REFACTO

    private val userByAccount = mutableMapOf<UserId, MutableList<Account>>()
    private val accountByTransaction = mutableMapOf<Long, MutableList<Transaction>>()
    private val userByTag = mutableMapOf<UserId, MutableList<Tag>>()
    private val accountList = mutableListOf<Account>()

    fun addAccount(ownerId: UserId, account: Account) {
        if(userByAccount[ownerId] == null) {
            userByAccount[ownerId] = mutableListOf()
        }
        userByAccount[ownerId]?.add(account)
        accountByTransaction[account.id!!] = mutableListOf()
    }
    fun removeAccountById(accountId: Long) {
        userByAccount.forEach {
            it.value.removeIf { account -> account.id == accountId }
        }
        accountByTransaction.remove(accountId)
    }

    fun upsert(account: Account) {
        val accountId = account.id
        userByAccount[account.owner?.id]?.find { it.id == accountId }?.updateFrom(account)
        accountByTransaction.computeIfAbsent(accountId!!) { mutableListOf() }
    }

    fun findAccountById(accountId: Long): Account? {

        var targetAccount: Account? = null
        userByAccount.forEach {
            val account = it.value.find { acc -> acc.id == accountId }
            if(account != null) {
                targetAccount = Account(accountId, account.amount, account.label, accountByTransaction[accountId]!!, initialSold = account.initialSold, previewAmount = account.previewAmount, owner = account.owner)
            }
        }
        return targetAccount
    }

    fun clearAccounts() {
        userByAccount.clear()
        accountByTransaction.clear()
    }

    private fun accountsWithTransactions(): List<Account> {
        val accountList = mutableSetOf<Account>()
        val result = mutableListOf<Account>()
        userByAccount.forEach { (_, value) -> accountList.addAll(value) }

        accountList.forEach {
            result.add(
                Account(it.id, it.amount, it.label, accountByTransaction[it.id]!!, initialSold = it.initialSold, previewAmount = it.previewAmount, owner = it.owner)
            )
        }
        return result
    }

    fun accountsByOwner(): Collection<AccountByOwner> {
        return userByAccount.map { AccountByOwner(it.value, it.key) }
    }

    fun initAccounts(initialState: Collection<AccountByOwner>) {
        initialState.forEach { accByOwn ->
            accByOwn.account.forEach {
                addAccount(accByOwn.userId, it)
            }
        }
    }

    fun addTransaction(userAccountId: IdUserAccount, transaction: Transaction) {
        val account = userByAccount[userAccountId.userId]?.find { it.id == userAccountId.accountId }
        account?.addTransaction(transaction)
        accountByTransaction[account!!.id]?.add(transaction)
    }
    fun addMassiveTransaction(collection: Collection<IdUserAccountByTransaction>){
        collection.forEach { idByTr ->
            idByTr.transactions.forEach {
                addTransaction(idByTr.id, it)
            }
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
        return accountsWithTransactions().flatMap { it.transactions }
            .find { it.id == transactionId }
    }

    fun clearUsers() {
        users.clear()
    }

    fun initUsers(userCollection: Collection<UserWithPassword>) {
        users.putAll(userCollection.associateBy { it.user.id })
    }

    fun findTransactions(): Collection<IdUserAccountByTransaction> {
        val transactionsResult = mutableMapOf<Pair<IdUserAccount, Long>, Transaction>()
        userByAccount.forEach { (key, value) ->
            value.forEach {
                val id = IdUserAccount(key, it.id!!)
                accountByTransaction[it.id]!!.forEach { transaction ->
                    transactionsResult[id to transaction.id!!] = transaction
                }

            }
        }
        val result = mutableMapOf<IdUserAccount, MutableList<Transaction>>()
        transactionsResult.forEach {
            if(result[it.key.first] == null) {
                result[it.key.first] = mutableListOf()
            }
            result[it.key.first]?.add(it.value)
        }
        return result.map { IdUserAccountByTransaction(it.key, it.value) }
    }

    fun findAccountByOwnerAndLabel(userId: UserId, accountLabel: String): Account?
        = accountsWithTransactions().find { it.owner?.id == userId && it.label == accountLabel }


    fun clearTransactions() {
        transactions.clear()
        accountByTransaction.clear()
    }

    fun saveTransaction(accountId: Long, transaction: Transaction) {
        accountByTransaction[accountId]?.add(transaction)
    }
}
