package fr.sacane.jmanager.domain.models

import java.time.Month

class Account(
        val id: Long?,
        private var amount: Double,
        private var labelAccount: String,
        val sheets: MutableList<Sheet>?
){

    val label: String
        get() = labelAccount

    val sold: Double
        get() = amount


    override fun equals(other: Any?): Boolean = (other is Account) && labelAccount == other.label
    fun sheets(): List<Sheet>?{
        if(sheets == null) return null
        return if(sheets.isEmpty()) null else sheets.toList()
    }

    fun updateFrom(account: Account) {
        amount = account.sold
        labelAccount = account.label
    }

    fun earn(earned: Double) {
        amount += earned
    }

    fun loss(loss: Double) {
        amount -= loss
    }

    override fun hashCode(): Int {
        return labelAccount.hashCode()
    }
    operator fun plusAssign(earned: Double){
        this.earn(earned)
    }
    operator fun minusAssign(loss: Double){
        this.loss(loss)
    }

    fun transaction(delta: Double, otherAccount: Account, isEntry: Boolean){
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
            ?.filter { it.date.month == month && it.date.year == year }
    }

    override fun toString(): String {
        return """
            id: $id
            amount: $amount
            label: $labelAccount
        """.trimIndent()
    }
}
