package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.BiState
import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.RegularByBooklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository

data class UserRegularTransaction(
    val userId: UserId,
    val transaction: RegularTransaction,
    val bookletIds: List<Long> = emptyList()
)

class InMemoryRegularTransactionRepository(
    private val inMemoryDatabase: InMemoryDatabase
): RegularTransactionRepository, BiState<List<UserRegularTransaction>, List<RegularTransaction>> {

    override fun getStates(): List<RegularTransaction> {
        return inMemoryDatabase.users.keys.flatMap { userId ->
            inMemoryDatabase.getAllRegularTransactionsByUser(userId)
        }
    }

    override fun clear() {
        inMemoryDatabase.clearRegularTransactions()
    }

    override fun init(initialState: List<UserRegularTransaction>) {
        // Clear all existing regular transactions first
        inMemoryDatabase.clearRegularTransactions()

        // Then initialize with new state
        val groupedByUser = initialState.groupBy { it.userId }
        groupedByUser.forEach { (userId, userTransactions) ->
            val regularByBooklets = userTransactions.map {
                RegularByBooklet(it.transaction, it.bookletIds)
            }
            inMemoryDatabase.initRegularTransactions(userId, regularByBooklets)
        }
    }

    override fun getAllRegularTransactions(userId: UserId): List<RegularTransaction> {
        return inMemoryDatabase.getAllRegularTransactionsByUser(userId)
    }

    override fun getAllRegularUsedByAccount(
        userId: UserId,
        accountID: Long
    ): List<RegularTransaction> {
        return inMemoryDatabase.getAllRegularTransactionsByBooklet(userId, accountID)
    }

    override fun saveMonthlyRegularTransaction(
        userId: UserId,
        monthlyTransaction: MonthlyTransaction,
        bookletIds: List<Long>
    ): RegularTransaction {
        inMemoryDatabase.addRegularBooklet(userId, monthlyTransaction, bookletIds)
        return monthlyTransaction
    }

    override fun getRegularTransactionById(
        userId: UserId,
        transactionId: RegularTransactionId
    ): RegularTransaction? {
        return inMemoryDatabase.getRegularTransactionById(userId, transactionId)
    }

}