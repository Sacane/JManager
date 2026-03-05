package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.fake.AccountByOwner
import fr.sacane.jmanager.domain.fake.IdUserAccount
import fr.sacane.jmanager.domain.fake.IdUserAccountByTransaction
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import java.util.UUID

data class RegularByBooklet(val transaction: RegularTransaction, val bookletIds: List<UUID>)

class InMemoryDatabase {
    val users = mutableMapOf<UserId, UserWithPassword>()

    private val userByBooklet = mutableMapOf<UserId, MutableList<Booklet>>()
    private val bookletsByTransaction = mutableMapOf<UUID, MutableList<Transaction>>()
    private val tags = mutableMapOf<UserId, MutableList<Tag>>()
    val userByTag = mutableMapOf<UserId, MutableList<Tag>>()
    val defaultTags = fr.sacane.jmanager.domain.models.defaultTags.map { tag ->
        Tag(
            id = UUID.randomUUID(),
            label = tag.label,
            isDefault = tag.isDefault,
            color = tag.color
        )
    }
    private val trackers = mutableMapOf<UUID, MutableList<RegularTransactionTracker>>()
    private val regularBooklets = mutableListOf<RegularByBooklet>()
    private val regularTransactionsByUser = mutableMapOf<UserId, MutableList<RegularByBooklet>>()

    fun addRegularBooklet(userId: UserId, transaction: RegularTransaction, bookletIds: List<UUID>) {
        val regularByBooklet = RegularByBooklet(transaction, bookletIds)
        regularBooklets.add(regularByBooklet)
        regularTransactionsByUser.computeIfAbsent(userId) { mutableListOf() }.add(regularByBooklet)
    }

    fun getAllRegularTransactionsByUser(userId: UserId): List<RegularTransaction> {
        return regularTransactionsByUser[userId]?.map { it.transaction } ?: emptyList()
    }

    fun getAllRegularTransactionsByBooklet(userId: UserId, bookletId: UUID): List<RegularTransaction> {
        return regularTransactionsByUser[userId]
            ?.filter { it.bookletIds.contains(bookletId) }
            ?.map { it.transaction }
            ?: emptyList()
    }

    fun getRegularTransactionById(userId: UserId, transactionId: RegularTransactionId): RegularTransaction? {
        return regularTransactionsByUser[userId]
            ?.map { it.transaction }
            ?.find { it.id == transactionId }
    }

    fun updateRegularTransaction(userId: UserId, regularTransaction: RegularTransaction) {
        val userTransactions = regularTransactionsByUser[userId] ?: return
        val index = userTransactions.indexOfFirst { it.transaction.id == regularTransaction.id }
        if (index != -1) {
            val oldBookletIds = userTransactions[index].bookletIds
            userTransactions[index] = RegularByBooklet(regularTransaction, oldBookletIds)
        }
    }

    fun deleteRegularTransaction(userId: UserId, transactionId: RegularTransactionId): Boolean {
        val userTransactions = regularTransactionsByUser[userId] ?: return false
        val removed = userTransactions.removeIf { it.transaction.id == transactionId }
        if (removed) {
            regularBooklets.removeIf { it.transaction.id == transactionId }
        }
        return removed
    }

    fun clearRegularTransactions() {
        regularBooklets.clear()
        regularTransactionsByUser.clear()
    }

    fun initRegularTransactions(userId: UserId, initialState: List<RegularByBooklet>) {
        regularTransactionsByUser[userId] = initialState.toMutableList()
        regularBooklets.addAll(initialState)
    }
    fun addTrackerByBooklet(bookletId: UUID, transactionTracker: RegularTransactionTracker) {
        trackers[bookletId]?.removeIf { it.regularTransactionId == transactionTracker.regularTransactionId }
        trackers.computeIfAbsent(bookletId) { mutableListOf() }.add(transactionTracker)
    }

    fun  findTrackerByBooklet(bookletId: UUID): List<RegularTransactionTracker>? = trackers[bookletId]

    fun findTrackerByBookletAndTransaction(bookletId: UUID, transactionId: RegularTransactionId): RegularTransactionTracker? = trackers[bookletId]?.find { it.regularTransactionId == transactionId }

    fun deleteTrackerByBookletId(bookletId: UUID) {
        trackers.remove(bookletId)
    }

    fun addAccount(ownerId: UserId, booklet: Booklet) {
        if(userByBooklet[ownerId] == null) {
            userByBooklet[ownerId] = mutableListOf()
        }
        userByBooklet[ownerId]?.add(booklet)
        bookletsByTransaction[booklet.id!!] = mutableListOf()
    }
    fun removeAccountById(accountId: UUID) {
        userByBooklet.forEach {
            it.value.removeIf { account -> account.id == accountId }
        }
        bookletsByTransaction.remove(accountId)
    }

    fun upsert(booklet: Booklet) {
        val accountId = booklet.id
        userByBooklet[booklet.owner?.id]?.find { it.id == accountId }?.updateFrom(booklet)
        bookletsByTransaction[accountId!!] = booklet.transactions.toMutableList()
    }

    fun findAccountById(accountId: UUID): Booklet? {

        var targetBooklet: Booklet? = null
        userByBooklet.forEach {
            val account = it.value.find { acc -> acc.id == accountId }
            if(account != null) {
                val transactions = bookletsByTransaction[accountId] ?: mutableListOf()
                targetBooklet = Booklet(account.amount, account.label, transactions, initialSold = account.initialSold, owner = account.owner, id = accountId)
            }
        }
        return targetBooklet
    }

    fun clearAccounts() {
        userByBooklet.clear()
        bookletsByTransaction.clear()
    }

    private fun accountsWithTransactions(): List<Booklet> {
        val bookletList = mutableSetOf<Booklet>()
        val result = mutableListOf<Booklet>()
        userByBooklet.forEach { (_, value) -> bookletList.addAll(value) }

        bookletList.forEach {
            result.add(
                Booklet(it.amount, it.label, bookletsByTransaction[it.id]!!, initialSold = it.initialSold, owner = it.owner, id = it.id)
            )
        }
        return result
    }

    fun accountsByOwner(): Collection<AccountByOwner> {
        return userByBooklet.map { AccountByOwner(it.value, it.key) }
    }

    fun initAccounts(initialState: Collection<AccountByOwner>) {
        initialState.forEach { accByOwn ->
            accByOwn.booklet.forEach {
                addAccount(accByOwn.userId, it)
            }
        }
    }

    fun addTransaction(userAccountId: IdUserAccount, transaction: Transaction) {
        val account = userByBooklet[userAccountId.userId]?.find { it.id == userAccountId.accountId } ?: throw IllegalArgumentException("Account not found")
        account.addTransaction(transaction)
        bookletsByTransaction[account.id]?.add(transaction)
    }
    fun addMassiveTransaction(collection: Collection<IdUserAccountByTransaction>){
        collection.forEach { idByTr ->
            idByTr.transactions.forEach {
                addTransaction(idByTr.id, it)
            }
        }
    }

    fun removeAllTransactionsById(transactionIds: List<UUID>) {
        bookletsByTransaction.forEach { (_, transactions) ->
            println(transactions.removeAll { transactionIds.contains(it.id) })
        }
    }

    fun findTransactionById(transactionId: UUID): Transaction? {
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
        val transactionsResult = mutableMapOf<Pair<IdUserAccount, UUID>, Transaction>()
        userByBooklet.forEach { (key, value) ->
            value.forEach {
                val id = IdUserAccount(key, it.id!!)
                bookletsByTransaction[it.id]!!.forEach { transaction ->
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

    fun findAccountByOwnerAndLabel(userId: UserId, accountLabel: String): Booklet?
            = accountsWithTransactions().find { it.owner?.id == userId && it.label == accountLabel }


    fun clearTransactions() {
        bookletsByTransaction.clear()
    }

    fun saveTransaction(accountId: UUID, transaction: Transaction) {
        if (bookletsByTransaction[accountId] == null) {
            bookletsByTransaction[accountId] = mutableListOf()
        }
        bookletsByTransaction[accountId]?.add(transaction)
    }
}