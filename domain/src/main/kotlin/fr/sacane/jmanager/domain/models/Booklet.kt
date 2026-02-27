package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import java.time.Month
import java.util.UUID

class Booklet(
    var amount: Amount,
    private var labelAccount: String,
    private val _transactions: MutableList<Transaction> = mutableListOf(),
    private val _regularTransactions: MutableList<RegularTransaction> = mutableListOf(),
    var owner : User? = null,
    val initialSold: Amount = amount.copy(),
    var previewAmount: Amount = amount.copy(),
    val id: UUID? = null
){

    val label: String
        get() = labelAccount

    val transactions: List<Transaction>
        get() = _transactions
    val regularTransactions: List<RegularTransaction>
        get() = _regularTransactions

    override fun equals(other: Any?): Boolean = (other is Booklet) && labelAccount == other.label
    fun sheets(): List<Transaction>{
        return transactions.toList()
    }
    fun findTransactionById(id : UUID) : Transaction? {
        return transactions.firstOrNull { it.id == id }
    }

    fun updateFrom(booklet: Booklet) {
        amount = booklet.amount
        labelAccount = booklet.label
        _transactions.replaceAll {
            Transaction(it.id, it.label, it.date, it.amount, it.isIncome, tag = it.tag)
        }
        previewAmount = booklet.previewAmount
    }

    override fun hashCode(): Int {
        return labelAccount.hashCode()
    }

    fun retrieveSheetSurroundAndSortedByDate(month: Month, year: Int): List<Transaction>{
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
            label: $labelAccount
            initialSold: $initialSold
            previewAmount: $previewAmount
            owner: ${owner?.id}
        """.trimIndent()
    }

    fun addTransaction(transaction: Transaction) {
        _transactions.add(transaction)
        if(transaction.isNotPreview) {
            this.amount = this.amount + if(transaction.isIncome) transaction.amount else transaction.amount.negate()
        }
        println(transaction.amount)
        this.previewAmount = this.previewAmount + if(transaction.isIncome) transaction.amount else transaction.amount.negate()
    }
    private fun removeTransaction(transaction: Transaction) {
        _transactions.removeIf { transaction.id == it.id }
        this.previewAmount =
            this.previewAmount - if (transaction.isIncome) transaction.amount else transaction.amount.negate()
        if (transaction.isNotPreview) {
            this.amount = this.amount - if (transaction.isIncome) transaction.amount else transaction.amount.negate()
        }
    }
    fun removeTransactionById(transactionId: UUID) {
        transactions.find { it.id == transactionId }?.let {
            if(it.isNotPreview) {
                this.amount = this.amount - if(it.isIncome) it.amount else it.amount.negate()
            }
            this.previewAmount = this.previewAmount - if(it.isIncome) it.amount else it.amount.negate()
        }
        _transactions.removeIf { tr -> transactionId == tr.id }
    }

    fun removeTransactionIf(sheetOnList: (s: Transaction) -> Boolean) {
        _transactions.filter(sheetOnList).forEach {
            removeTransaction(it)
        }
    }
}
