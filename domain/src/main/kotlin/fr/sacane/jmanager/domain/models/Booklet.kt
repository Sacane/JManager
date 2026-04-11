package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import java.time.Month
import java.util.UUID

class Booklet(
    var amount: Amount,
    label: String,
    private val _transactions: MutableList<Transaction> = mutableListOf(),
    private val _regularTransactions: MutableList<RegularTransaction> = mutableListOf(),
    var owner : User? = null,
    val initialSold: Amount = amount.copy(),
    val id: UUID? = null,
    var monthlyPeriodStartDay: Int = 1,
    var monthlyPeriodEndDay: Int? = null,
){

    init {
        require(monthlyPeriodStartDay in 1..31) {
            "monthlyPeriodStartDay must be between 1 and 31"
        }
        require(monthlyPeriodEndDay == null || monthlyPeriodEndDay in 1..31) {
            "monthlyPeriodEndDay must be between 1 and 31 when provided"
        }
    }

    var label: String = label
        private set

    val transactions: List<Transaction>
        get() = _transactions
    val regularTransactions: List<RegularTransaction>
        get() = _regularTransactions

    override fun equals(other: Any?): Boolean = (other is Booklet) && label == other.label
    fun transactionsSnapshot(): List<Transaction>{
        return transactions.toList()
    }
    fun findTransactionById(id : UUID) : Transaction? {
        return transactions.firstOrNull { it.id == id }
    }

    fun updateFrom(booklet: Booklet) {
        amount = booklet.amount
        label = booklet.label
        monthlyPeriodStartDay = booklet.monthlyPeriodStartDay
        monthlyPeriodEndDay = booklet.monthlyPeriodEndDay
        _transactions.replaceAll {
            Transaction(it.id, it.label, it.date, it.amount, it.isIncome, tag = it.tag)
        }
    }

    fun updateMonthlyPeriodConfiguration(startDay: Int, endDay: Int?) {
        require(startDay in 1..31) {
            "monthlyPeriodStartDay must be between 1 and 31"
        }
        require(endDay == null || endDay in 1..31) {
            "monthlyPeriodEndDay must be between 1 and 31 when provided"
        }

        monthlyPeriodStartDay = startDay
        monthlyPeriodEndDay = endDay
    }

    fun updateMonthlyPeriodStartDay(day: Int) {
        updateMonthlyPeriodConfiguration(day, monthlyPeriodEndDay)
    }

    fun updateMonthlyPeriodEndDay(day: Int?) {
        updateMonthlyPeriodConfiguration(monthlyPeriodStartDay, day)
    }

    override fun hashCode(): Int {
        return label.hashCode()
    }

    fun retrieveTransactionsSortedByDate(month: Month, year: Int): List<Transaction>{
        val (standardTransaction, previewTransaction) = transactions
            .filter { it.date.month == month && it.date.year == year }
            .sortedWith(compareBy<Transaction>{it.date}.thenBy { it.lastModified })
            .partition { !it.isPreview }
        return standardTransaction + previewTransaction
    }

    override fun toString(): String {
        return """
            id: $id
            amount: $amount
            label: $label
            initialSold: $initialSold
            monthlyPeriodStartDay: $monthlyPeriodStartDay
            monthlyPeriodEndDay: $monthlyPeriodEndDay
            owner: ${owner?.id}
        """.trimIndent()
    }

    fun addTransaction(transaction: Transaction) {
        _transactions.add(transaction)
        if(transaction.isNotPreview) {
            this.amount = this.amount + if(transaction.isIncome) transaction.amount else transaction.amount.negate()
        }
    }
    private fun removeTransaction(transaction: Transaction) {
        _transactions.removeIf { transaction.id == it.id }
        if (transaction.isNotPreview) {
            this.amount = this.amount - if (transaction.isIncome) transaction.amount else transaction.amount.negate()
        }
    }
    fun removeTransactionById(transactionId: UUID) {
        transactions.find { it.id == transactionId }?.let {
            if(it.isNotPreview) {
                this.amount = this.amount - if(it.isIncome) it.amount else it.amount.negate()
            }
        }
        _transactions.removeIf { tr -> transactionId == tr.id }
    }

    fun removeTransactionIf(transactionOnList: (s: Transaction) -> Boolean) {
        _transactions.filter(transactionOnList).forEach {
            removeTransaction(it)
        }
    }
}
