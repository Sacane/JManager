package fr.sacane.jmanager.domain.models

import java.time.Month

class Account(
    val id: Long? = null,
    private var amount: Amount,
    private var labelAccount: String,
    val transactions: MutableList<Transaction> = mutableListOf(),
    val owner : User? = null
){

    val label: String
        get() = labelAccount

    val sold: Amount
        get() = amount

    fun updateSoldByLastSheet(): Boolean {
        this.amount = transactions.maxByOrNull { it.position }?.accountAmount ?: return false
        return true
    }

    override fun equals(other: Any?): Boolean = (other is Account) && labelAccount == other.label
    fun sheets(): List<Transaction>{
        return transactions.toList()
    }

    fun updateFrom(account: Account) {
        amount = account.sold
        labelAccount = account.label
        var addition = account.sold
        transactions.replaceAll {
            addition = if(it.isIncome) addition + it.amount else addition - it.amount
            Transaction(it.id, it.label, it.date, it.amount, it.isIncome, addition, it.tag, it.position)
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
    fun retrieveSheetSurroundByDate(month: Month, year: Int): List<Transaction>{
        return transactions
            .filter { it.date.month == month && it.date.year == year }
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
}
