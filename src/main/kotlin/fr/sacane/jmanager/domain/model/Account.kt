package fr.sacane.jmanager.domain.model



class Account(
        private var amount: Double,
        private var labelAccount: String,
        private var sheets: List<Sheet>
){

    fun sheets(): List<Sheet>{
        return sheets.distinct()
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
}
