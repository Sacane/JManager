package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.BiState
import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.Frequency
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository
import java.time.LocalDate
import java.util.*

data class UserRegularTransaction(
    val userId: UserId,
    val transaction: RegularTransaction
)

class InMemoryRegularTransactionRepository: RegularTransactionRepository, BiState<List<UserRegularTransaction>, List<RegularTransaction>> {

    private val transactions = mutableListOf<UserRegularTransaction>()
    private val transactionsByAccount = mutableMapOf<Long, MutableList<UserRegularTransaction>>()

    override fun getStates(): List<RegularTransaction> {
        return transactions.filter { it.transaction.id!!.value == UUID.randomUUID().toString() }.map { it.transaction }
    }

    override fun clear() {
        transactions.clear()
    }

    override fun init(initialState: List<UserRegularTransaction>) {
        transactions.addAll(initialState)
    }

    override fun getAllRegularTransactions(userId: UserId): List<RegularTransaction> {
        return transactions.filter { it.userId == userId }.map { it.transaction }
    }

    override fun getAllRegularUsedByAccount(
        userId: UserId,
        accountID: Long
    ): List<RegularTransaction>? {
        return transactionsByAccount[accountID]?.filter { it.userId == userId }?.map { it.transaction }
    }


    override fun saveMonthlyRegularTransaction(
        userId: UserId,
        monthlyTransaction: MonthlyTransaction,
        bookletIds: List<Long>
    ): RegularTransaction {
        transactions.add(UserRegularTransaction(userId, monthlyTransaction))
        bookletIds.forEach {
            transactionsByAccount.computeIfAbsent(it) { mutableListOf() }.add(UserRegularTransaction(userId, monthlyTransaction))
        }
        return monthlyTransaction
    }

    override fun getRegularTransactionById(
        userId: UserId,
        transactionId: RegularTransactionId
    ): RegularTransaction? {
        return transactions.find { it.userId == userId && it.transaction.id == transactionId }?.transaction
    }

}