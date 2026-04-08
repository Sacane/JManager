package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.fake.BookletsByOwner
import fr.sacane.jmanager.domain.fake.IdUserBooklet
import fr.sacane.jmanager.domain.fake.IdBookletByTransaction
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

    fun updateRegularTransaction(userId: UserId, regularTransaction: RegularTransaction, bookletIds: List<UUID>) {
        val userTransactions = regularTransactionsByUser[userId] ?: return
        val index = userTransactions.indexOfFirst { it.transaction.id == regularTransaction.id }
        if (index != -1) {
            userTransactions[index] = RegularByBooklet(regularTransaction, bookletIds)
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

    fun deleteTrackerByRegularTransactionId(regularTransactionId: RegularTransactionId) {
        trackers.forEach { (_, trackerList) ->
            trackerList.removeIf { it.regularTransactionId == regularTransactionId }
        }
    }

    fun deleteTrackerByPair(regularTransactionId: RegularTransactionId, bookletId: UUID) {
        trackers[bookletId]?.removeIf { it.regularTransactionId == regularTransactionId }
    }

    fun getBookletIdsForRegularTransaction(userId: UserId, transactionId: RegularTransactionId): List<UUID> {
        return regularTransactionsByUser[userId]
            ?.find { it.transaction.id == transactionId }
            ?.bookletIds
            ?: emptyList()
    }

    fun addBooklet(ownerId: UserId, booklet: Booklet) {
        if(userByBooklet[ownerId] == null) {
            userByBooklet[ownerId] = mutableListOf()
        }
        userByBooklet[ownerId]?.add(booklet)
        bookletsByTransaction[booklet.id!!] = mutableListOf()
    }
    fun removeBookletById(bookletId: UUID) {
        userByBooklet.forEach {
            it.value.removeIf { booklet -> booklet.id == bookletId }
        }
        bookletsByTransaction.remove(bookletId)
    }

    fun upsert(booklet: Booklet) {
        val bookletId = booklet.id
        userByBooklet[booklet.owner?.id]?.find { it.id == bookletId }?.updateFrom(booklet)
        bookletsByTransaction[bookletId!!] = booklet.transactions.toMutableList()
    }

    fun findBookletById(bookletId: UUID): Booklet? {

        var targetBooklet: Booklet? = null
        userByBooklet.forEach {
            val found = it.value.find { acc -> acc.id == bookletId }
            if(found != null) {
                val transactions = bookletsByTransaction[bookletId] ?: mutableListOf()
                targetBooklet = Booklet(
                    found.amount,
                    found.label,
                    transactions,
                    initialSold = found.initialSold,
                    owner = found.owner,
                    id = bookletId,
                    monthlyPeriodStartDay = found.monthlyPeriodStartDay,
                    monthlyPeriodEndDay = found.monthlyPeriodEndDay,
                )
            }
        }
        return targetBooklet
    }

    fun clearBooklets() {
        userByBooklet.clear()
        bookletsByTransaction.clear()
    }

    private fun bookletsWithTransactions(): List<Booklet> {
        val bookletList = mutableSetOf<Booklet>()
        val result = mutableListOf<Booklet>()
        userByBooklet.forEach { (_, value) -> bookletList.addAll(value) }

        bookletList.forEach {
            result.add(
                Booklet(
                    it.amount,
                    it.label,
                    bookletsByTransaction[it.id]!!,
                    initialSold = it.initialSold,
                    owner = it.owner,
                    id = it.id,
                    monthlyPeriodStartDay = it.monthlyPeriodStartDay,
                    monthlyPeriodEndDay = it.monthlyPeriodEndDay,
                )
            )
        }
        return result
    }

    fun bookletsByOwner(): Collection<BookletsByOwner> {
        return userByBooklet.map { BookletsByOwner(it.value, it.key) }
    }

    fun initBooklets(initialState: Collection<BookletsByOwner>) {
        initialState.forEach { accByOwn ->
            accByOwn.booklets.forEach {
                addBooklet(accByOwn.userId, it)
            }
        }
    }

    fun addTransaction(userBookletId: IdUserBooklet, transaction: Transaction) {
        val booklet = userByBooklet[userBookletId.userId]?.find { it.id == userBookletId.bookletId } ?: throw IllegalArgumentException("Booklet not found")
        booklet.addTransaction(transaction)
        bookletsByTransaction[booklet.id]?.add(transaction)
    }
    fun addMassiveTransaction(collection: Collection<IdBookletByTransaction>){
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
        return bookletsWithTransactions().flatMap { it.transactions }
            .find { it.id == transactionId }
    }

    fun clearUsers() {
        users.clear()
    }

    fun initUsers(userCollection: Collection<UserWithPassword>) {
        users.putAll(userCollection.associateBy { it.user.id })
    }

    fun findTransactions(): Collection<IdBookletByTransaction> {
        val transactionsResult = mutableMapOf<Pair<IdUserBooklet, UUID>, Transaction>()
        userByBooklet.forEach { (key, value) ->
            value.forEach {
                val id = IdUserBooklet(key, it.id!!)
                bookletsByTransaction[it.id]!!.forEach { transaction ->
                    transactionsResult[id to transaction.id!!] = transaction
                }

            }
        }
        val result = mutableMapOf<IdUserBooklet, MutableList<Transaction>>()
        transactionsResult.forEach {
            if(result[it.key.first] == null) {
                result[it.key.first] = mutableListOf()
            }
            result[it.key.first]?.add(it.value)
        }
        return result.map { IdBookletByTransaction(it.key, it.value) }
    }

    fun findBookletByOwnerAndLabel(userId: UserId, bookletLabel: String): Booklet?
            = bookletsWithTransactions().find { it.owner?.id == userId && it.label == bookletLabel }


    fun clearTransactions() {
        bookletsByTransaction.clear()
    }

    fun saveTransaction(bookletId: UUID, transaction: Transaction) {
        if (bookletsByTransaction[bookletId] == null) {
            bookletsByTransaction[bookletId] = mutableListOf()
        }
        bookletsByTransaction[bookletId]?.add(transaction)
    }

    fun isPersonalTagUsed(tagId: UUID): Boolean {
        return bookletsByTransaction.values.flatten().any { t ->
            t.tag?.id == tagId && t.tag?.isDefault == false
        }
    }

    fun replacePersonalTagByDefault(tagId: UUID, defaultTag: Tag) {
        bookletsByTransaction.forEach { (_, transactions) ->
            transactions.replaceAll { t ->
                if (t.tag?.id == tagId && t.tag?.isDefault == false) t.copy(tag = defaultTag) else t
            }
        }
    }

    fun isRegularPersonalTagUsed(tagId: UUID): Boolean {
        return regularTransactionsByUser.values.flatten().any { rb ->
            rb.transaction.tag?.id == tagId && rb.transaction.tag?.isDefault == false
        }
    }

    fun replaceRegularPersonalTagByDefault(tagId: UUID, defaultTag: Tag) {
        regularTransactionsByUser.forEach { (_, rbList) ->
            rbList.replaceAll { rb ->
                if (rb.transaction.tag?.id == tagId && rb.transaction.tag?.isDefault == false) {
                    rb.copy(transaction = rb.transaction.copy(tag = defaultTag))
                } else rb
            }
        }
    }
}