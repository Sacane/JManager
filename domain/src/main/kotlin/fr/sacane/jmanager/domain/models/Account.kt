package fr.sacane.jmanager.domain.models

import java.time.Month

class Account(
    val id: Long? = null,
    var sold: Amount,
    private var labelAccount: String,
    val transactions: MutableList<Transaction> = mutableListOf(),
    val owner : User? = null,
    val initialSold: Amount = sold,
    val previewAmount: Amount = sold
){

    val label: String
        get() = labelAccount

    override fun equals(other: Any?): Boolean = (other is Account) && labelAccount == other.label
    fun sheets(): List<Transaction>{
        return transactions.toList()
    }

    fun updateFrom(account: Account) {
        sold = account.sold
        labelAccount = account.label
        transactions.replaceAll {
            Transaction(it.id, it.label, it.date, it.amount, it.isIncome, it.tag)
        }
    }

    override fun hashCode(): Int {
        return labelAccount.hashCode()
    }
    operator fun plusAssign(earned: Amount){
        this.sold = this.sold + earned
    }
    operator fun minusAssign(loss: Amount){
        this.sold = this.sold - loss
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
            amount: $sold
            label: $labelAccount
            initialSold: $initialSold
        """.trimIndent()
    }
    fun cancelSheetsAmount(transactions: List<Transaction>) {
        this.transactions.removeAll { it.id in transactions.map { tr -> tr.id } }
        transactions.forEach {
            this.sold = if(it.isIncome) this.sold - it.amount else it.amount + this.sold
        }
    }
    fun updateSoldFromTransactions(oldTransaction: Transaction, newTransaction: Transaction) {
         // First modification
        this.sold = if(oldTransaction.isIncome) sold - oldTransaction.amount else sold + oldTransaction.amount
        // Second modification
        this.sold = if(newTransaction.isIncome) sold + newTransaction.amount else sold - newTransaction.amount
    }

    fun addTransaction(transaction: Transaction) {
        this.transactions.add(transaction)
        this.sold = this.sold + if(transaction.isIncome) transaction.amount else transaction.amount.negate()
    }
    fun addAllTransaction(transactions: List<Transaction>) {
        this.sold = 0.toAmount()
        this.transactions.removeAll { it.id in transactions.map { tr -> tr.id } }
        transactions.forEach {
            addTransaction(it)
        }
    }
}
