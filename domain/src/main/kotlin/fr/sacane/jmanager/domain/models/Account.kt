package fr.sacane.jmanager.domain.models

import java.time.Month

class Account(
        val id: Long? = null,
        private var amount: Amount,
        private var labelAccount: String,
        val sheets: MutableList<Sheet> = mutableListOf()
){

    val label: String
        get() = labelAccount

    val sold: Amount
        get() = amount

    fun updateSoldByLastSheet(): Boolean {
        this.amount = sheets.maxByOrNull { it.position }?.sold ?: return false
        return true
    }

    override fun equals(other: Any?): Boolean = (other is Account) && labelAccount == other.label
    fun sheets(): List<Sheet>{
        return sheets.toList()
    }

    fun addSheet(sheet: Sheet) {
        sheets.add(sheet)
    }

    fun updateFrom(account: Account) {
        amount = account.sold
        labelAccount = account.label
        var addition = account.sold
        //TODO remplacer les sheets lors de la modification d'un comptes
        sheets.replaceAll {
            addition = if(it.expenses == Amount(0.toBigDecimal())) addition - it.income else addition + it.expenses
            Sheet(it.id, it.label, it.date, it.expenses, it.income, addition, it.category, it.position)
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
    fun retrieveSheetSurroundByDate(month: Month, year: Int): List<Sheet>?{
        return sheets
            .filter { it.date.month == month && it.date.year == year }
    }

    override fun toString(): String {
        return """
            id: $id
            amount: $amount
            label: $labelAccount
        """.trimIndent()
    }
    fun cancelSheetsAmount(sheets: List<Sheet>) {
        sheets.forEach {
            this.amount = this.amount
            .plus(it.expenses)
            .minus(it.income)
        }
    }
    fun setSoldFromSheet(sheetFromResource: Sheet) {
        this.amount = sheetFromResource.sold
    }
}
