package fr.sacane.jmanager.domain.fake

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

class InMemoryRegularTransactionRepository: RegularTransactionRepository, State<RegularTransaction> {

    private val transactions = mutableListOf<RegularTransaction>()
    private val transactionsByAccount = mutableMapOf<Long, MutableList<RegularTransaction>>()

    override fun getStates(): Collection<RegularTransaction> {
        return transactions
    }

    override fun clear() {
        transactions.clear()
    }

    override fun init(initialState: Collection<RegularTransaction>) {
        transactions.addAll(initialState)
    }

    override fun saveRegularTransaction(
        userId: UserId,
        startDate: LocalDate,
        label: String,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag,
        frequency: Frequency
    ): RegularTransaction {
        val transaction = MonthlyTransaction(
            id = RegularTransactionId("${userId.value}-${UUID.randomUUID()}"),
            startDate = startDate,
            label = label,
            amount = amount,
            isIncome = isIncome,
            tag = tag,
            frequencyProperty = FrequencyProperty.Forever(),
        )
        transactions.add(transaction)
        return transaction
    }

    override fun getAllRegularTransactions(userId: UserId): List<RegularTransaction> {
        return transactions.filter { it.id!!.value.startsWith("${userId.value}") }
    }

    override fun getAllRegularUsedByAccount(
        userId: UserId,
        accountID: Long
    ): List<RegularTransaction> {
        return transactionsByAccount[accountID] ?: emptyList()
    }

    override fun linkedRegularTransactionsWithBooklet(
        userId: UserId,
        regularTransactionId: RegularTransactionId,
        bookletId: Long
    ){
        val regularTransactions = transactionsByAccount[bookletId] ?: return
        regularTransactions.forEach { transaction ->
            if (transaction.id!!.value.startsWith("${userId.value}")) {
                transactionsByAccount.computeIfAbsent(bookletId) { mutableListOf() }.add(transaction)
            }
        }
    }

    override fun saveRegularTransaction(
        userId: UserId,
        transaction: RegularTransaction
    ): RegularTransaction {
        transactions.add(transaction)
        return transaction
    }

    override fun saveMonthlyRegularTransaction(
        userId: UserId,
        monthlyTransaction: MonthlyTransaction
    ): RegularTransaction {
        transactions.add(monthlyTransaction)
        return monthlyTransaction
    }

    override fun getRegularTransactionById(
        userId: UserId,
        transactionId: RegularTransactionId
    ): RegularTransaction {
        return transactions.find { it.id == transactionId } ?: throw NoSuchElementException("No transaction found with id $transactionId")
    }

}