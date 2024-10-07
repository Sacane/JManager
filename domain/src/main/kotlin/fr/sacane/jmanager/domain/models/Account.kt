package fr.sacane.jmanager.domain.models

import java.time.Month

class Account(
    val id: Long? = null,
    var amount: Amount,
    private var labelAccount: String,
    val transactions: MutableList<Transaction> = mutableListOf(),
    val owner : User? = null,
    val initialSold: Amount = amount,
    var previewAmount: Amount = amount,
    val periodicalTransactions: MutableList<PeriodicalTransaction> = mutableListOf()
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

    fun retrieveSheetSurroundAndSortedByDate(month: Month, year: Int, searchIsPreview: Boolean = false): List<Transaction>{
        return transactions
            .filter { it.date.month == month && it.date.year == year && it.isPreview == searchIsPreview }
            .sortedWith(compareBy<Transaction>{it.date}.thenBy { it.lastModified })
    }

    override fun toString(): String {
        return """
            id: $id
            amount: $amount
            label: $labelAccount
            initialSold: $initialSold
        """.trimIndent()
    }
    fun cancelSheetsAmount(transactions: List<Transaction>) {
        this.transactions.removeAll { it.id in transactions.map { tr -> tr.id } }
        transactions.forEach {
            this.amount = if(it.isIncome) this.amount - it.amount else it.amount + this.amount
        }
    }
    fun updateSoldFromTransactions(oldTransaction: Transaction, newTransaction: Transaction) {
         // First modification
        this.amount = if(oldTransaction.isIncome) amount - oldTransaction.amount else amount + oldTransaction.amount
        // Second modification
        this.amount = if(newTransaction.isIncome) amount + newTransaction.amount else amount - newTransaction.amount
    }

    fun addTransaction(transaction: Transaction) {
        if(transactions.find { it.id == transaction.id } == null) {
            transactions.add(transaction)
            if(transaction.isPreview) {
                this.previewAmount = this.previewAmount + if(transaction.isIncome) transaction.amount else transaction.amount.negate()
            } else {
                this.amount = this.amount + if(transaction.isIncome) transaction.amount else transaction.amount.negate()
            }
        }
    }
    fun removeTransaction(transaction: Transaction) {
        transactions.removeIf { transaction.id == it.id }
        if(transaction.isPreview) {
            this.previewAmount = this.previewAmount - if(transaction.isIncome) transaction.amount else transaction.amount.negate()
        } else {
            this.amount = this.amount - if(transaction.isIncome) transaction.amount else transaction.amount.negate()
        }
    }
    fun removeAllTransactions(transactions: List<Transaction>) {
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
}
