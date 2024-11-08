package fr.sacane.jmanager.domain.models

import java.time.Month

class Account(
    val id: Long? = null,
    var amount: Amount,
    private var labelAccount: String,
    val transactions: MutableList<Transaction> = mutableListOf(),
    val owner : User? = null,
    val initialSold: Amount = amount.copy(),
    var previewAmount: Amount = amount.copy(),
){

    val label: String
        get() = labelAccount

    override fun equals(other: Any?): Boolean = (other is Account) && labelAccount == other.label
    fun sheets(): List<Transaction>{
        return transactions.toList()
    }

    fun updateFrom(account: Account) {
        amount = account.amount
        labelAccount = account.label
        transactions.replaceAll {
            Transaction(it.id, it.label, it.date, it.amount, it.isIncome, it.tag)
        }
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
        """.trimIndent()
    }

    fun updateSoldFromTransactions(oldTransaction: Transaction, newTransaction: Transaction) {
         // First modification
        this.amount = if(oldTransaction.isIncome) amount - oldTransaction.amount else amount + oldTransaction.amount
        // Second modification
        this.amount = if(newTransaction.isIncome) amount + newTransaction.amount else amount - newTransaction.amount
    }

    fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
        if(transaction.isNotPreview) {
            this.amount = this.amount + if(transaction.isIncome) transaction.amount else transaction.amount.negate()
        }
        this.previewAmount = this.previewAmount + if(transaction.isIncome) transaction.amount else transaction.amount.negate()
    }
    private fun removeTransaction(transaction: Transaction) {
        transactions.removeIf { transaction.id == it.id }
        this.previewAmount = this.previewAmount - if(transaction.isIncome) transaction.amount else transaction.amount.negate()
        if(transaction.isNotPreview) {
            this.amount = this.amount - if(transaction.isIncome) transaction.amount else transaction.amount.negate()
        }
    }
    fun removeTransactionById(transactionId: Long) {
        transactions.find { it.id == transactionId }?.let {
            if(it.isNotPreview) {
                this.amount = this.amount - if(it.isIncome) it.amount else it.amount.negate()
            }
            this.previewAmount = this.previewAmount - if(it.isIncome) it.amount else it.amount.negate()
        }
        transactions.removeIf { tr -> transactionId == tr.id }
    }
    private fun removeAllTransactions(transactions: List<Transaction>) {
        for(transaction in transactions){
            removeTransaction(transaction)
        }
    }

    fun addAllTransaction(transactions: List<Transaction>) {
        removeAllTransactions(transactions)
        transactions.forEach {
            addTransaction(it)
        }
    }

    fun removeTransactionIf(sheetOnList: (s: Transaction) -> Boolean) {
        transactions.filter(sheetOnList).forEach {
            removeTransaction(it)
        }
    }
}
