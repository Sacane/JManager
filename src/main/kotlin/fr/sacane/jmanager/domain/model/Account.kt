package fr.sacane.jmanager.domain.model

class Account(
        private var amount: Double,
        private val labelAccount: String,
        private val sheets: MutableList<Sheet>
){

    override fun equals(other: Any?): Boolean = (other is Account) && labelAccount == other.label()
    fun sheets(): List<Sheet>?{
        return if(sheets.isEmpty()) null else sheets.toList()
    }
    fun amount(): Double = amount

    fun label(): String{
        return labelAccount
    }


    fun earnAmount(earned: Double) {
        amount += earned
    }

    fun lossAmount(loss: Double) {
        amount -= loss
    }

    override fun hashCode(): Int {
        return labelAccount.hashCode()
    }
}
