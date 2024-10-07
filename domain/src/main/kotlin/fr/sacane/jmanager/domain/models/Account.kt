package fr.sacane.jmanager.domain.models

import java.time.Month

class Account(
    val id: Long? = null,
    var amount: Amount,
    private var labelAccount: String,
    val transactions: MutableList<Transaction> = mutableListOf(),
    val owner : User? = null,
    val initialSold: Amount = amount,
    val previewAmount: Amount = amount
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
    operator fun plusAssign(earned: Amount){
        this.amount = this.amount + earned
    }
    operator fun minusAssign(loss: Amount){
        this.amount = this.amount - loss
    }

    fun transaction(delta: Amount, otherAccount: Account, isEntry: Boolean){
        if(isEntry){
            this += delta
            otherAccount -= delta
        } else {
            this -= delta
            otherAccount += delta
        }
    }
    fun retrieveSheetSurroundAndSortedByDate(month: Month, year: Int): List<Transaction>{
        return transactions
            .filter { it.date.month == month && it.date.year == year }
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
        this.transactions.add(transaction)
        this.amount = this.amount + if(transaction.isIncome) transaction.amount else transaction.amount.negate()
    }
    fun addAllTransaction(transactions: List<Transaction>) {
        this.amount = 0.toAmount()
        this.transactions.removeAll { it.id in transactions.map { tr -> tr.id } }
        transactions.forEach {
            addTransaction(it)
        }
    }
}
