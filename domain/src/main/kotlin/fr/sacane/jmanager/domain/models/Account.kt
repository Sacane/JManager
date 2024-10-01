package fr.sacane.jmanager.domain.models

import java.time.Month

class Account(
    val id: Long? = null,
    private var amount: Amount,
    private var labelAccount: String,
    val transactions: MutableList<Transaction> = mutableListOf(),
    val owner : User? = null,
    val initialSold: Amount = amount
){

    val label: String
        get() = labelAccount

    val sold: Amount
        get() = amount


    override fun equals(other: Any?): Boolean = (other is Account) && labelAccount == other.label
    fun sheets(): List<Transaction>{
        return transactions.toList()
    }

    fun updateFrom(account: Account) {
        amount = account.sold
        labelAccount = account.label
        transactions.replaceAll {
            Transaction(it.id, it.label, it.date, it.amount, it.isIncome, it.tag, it.position)
        }
    }

    fun transactionsByMonthSortedByDate(month: Month): List<Transaction> {
        return transactions.filter { it.date.month == month }.sortedBy { it.date }
    }

    fun transactionsFilterAndSortedByPositionBefore(position: Int): List<Transaction> {
        return transactions.filter { it.position <= position }.sortedBy { position }
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
            .sortedBy { it.position }
    }

    override fun toString(): String {
        return """
            id: $id
            amount: $amount
            label: $labelAccount
        """.trimIndent()
    }
    fun cancelSheetsAmount(transactions: List<Transaction>) {
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
}
