package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.fake.AccountByOwner
import fr.sacane.jmanager.domain.fake.IdUserAccount
import fr.sacane.jmanager.domain.fake.IdUserAccountByTransaction
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.models.transaction.Transaction

class InMemoryDatabase {
    val users = mutableMapOf<UserId, UserWithPassword>()

    private val userByBooklet = mutableMapOf<UserId, MutableList<Booklet>>()
    private val accountByTransaction = mutableMapOf<Long, MutableList<Transaction>>()
    private val tags = mutableMapOf<UserId, MutableList<Tag>>()
    val userByTag = mutableMapOf<UserId, MutableList<Tag>>()
    private val bookletList = mutableListOf<Booklet>()
    val defaultTags = mutableListOf<Tag>()

    fun addAccount(ownerId: UserId, booklet: Booklet) {
        if(userByBooklet[ownerId] == null) {
            userByBooklet[ownerId] = mutableListOf()
        }
        userByBooklet[ownerId]?.add(booklet)
        accountByTransaction[booklet.id!!] = mutableListOf()
    }
    fun removeAccountById(accountId: Long) {
        userByBooklet.forEach {
            it.value.removeIf { account -> account.id == accountId }
        }
        accountByTransaction.remove(accountId)
    }

    fun upsert(booklet: Booklet) {
        val accountId = booklet.id
        userByBooklet[booklet.owner?.id]?.find { it.id == accountId }?.updateFrom(booklet)
        accountByTransaction.computeIfAbsent(accountId!!) { mutableListOf() }
    }

    fun findAccountById(accountId: Long): Booklet? {

        var targetBooklet: Booklet? = null
        userByBooklet.forEach {
            val account = it.value.find { acc -> acc.id == accountId }
            if(account != null) {
                targetBooklet = Booklet(account.amount, account.label, accountByTransaction[accountId]!!, initialSold = account.initialSold, previewAmount = account.previewAmount, owner = account.owner, id = accountId)
            }
        }
        return targetBooklet
    }

    fun clearAccounts() {
        userByBooklet.clear()
        accountByTransaction.clear()
    }

    private fun accountsWithTransactions(): List<Booklet> {
        val bookletList = mutableSetOf<Booklet>()
        val result = mutableListOf<Booklet>()
        userByBooklet.forEach { (_, value) -> bookletList.addAll(value) }

        bookletList.forEach {
            result.add(
                Booklet(it.amount, it.label, accountByTransaction[it.id]!!, initialSold = it.initialSold, previewAmount = it.previewAmount, owner = it.owner, id = it.id)
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
        val account = userByBooklet[userAccountId.userId]?.find { it.id == userAccountId.accountId }
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

    }
    fun removeAllTransactionsById(transactionIds: List<Long>) {
        accountByTransaction.forEach { (_, transactions) ->
            println(transactions.removeAll { transactionIds.contains(it.id) })
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
        userByBooklet.forEach { (key, value) ->
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

    fun findAccountByOwnerAndLabel(userId: UserId, accountLabel: String): Booklet?
            = accountsWithTransactions().find { it.owner?.id == userId && it.label == accountLabel }


    fun clearTransactions() {
        accountByTransaction.clear()
    }

    fun saveTransaction(accountId: Long, transaction: Transaction) {
        accountByTransaction[accountId]?.add(transaction)
    }

    fun addTag(userId: UserId, tag: Tag) {
        if(tags[userId] == null) {
            tags[userId] = mutableListOf()
        }
        tags[userId]?.add(tag)
    }
}