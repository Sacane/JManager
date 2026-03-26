package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.BiState
import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.RegularByBooklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import java.util.UUID

data class UserRegularTransaction(
    val userId: UserId,
    val transaction: RegularTransaction,
    val bookletIds: List<UUID> = emptyList()
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
        inMemoryDatabase.clearRegularTransactions()

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
        accountID: UUID
    ): List<RegularTransaction> {
        return inMemoryDatabase.getAllRegularTransactionsByBooklet(userId, accountID)
    }

    override fun saveRegularTransaction(
        userId: UserId,
        regularTransaction: RegularTransaction,
        bookletIds: List<UUID>
    ): RegularTransaction {
        inMemoryDatabase.addRegularBooklet(userId, regularTransaction, bookletIds)
        return regularTransaction
    }

    override fun getRegularTransactionById(
        userId: UserId,
        transactionId: RegularTransactionId
    ): RegularTransaction? {
        return inMemoryDatabase.getRegularTransactionById(userId, transactionId)
    }

    override fun updateRegularTransaction(
        userId: UserId,
        regularTransaction: RegularTransaction,
        bookletIds: List<UUID>
    ): RegularTransaction? {
        val existing = inMemoryDatabase.getRegularTransactionById(userId, regularTransaction.id)
        if (existing == null) {
            return null
        }

        inMemoryDatabase.updateRegularTransaction(userId, regularTransaction, bookletIds)
        return regularTransaction
    }

    override fun deleteRegularTransaction(
        userId: UserId,
        transactionId: RegularTransactionId
    ): Boolean {
        return inMemoryDatabase.deleteRegularTransaction(userId, transactionId)
    }

}
