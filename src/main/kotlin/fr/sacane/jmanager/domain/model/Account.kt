package fr.sacane.jmanager.domain.model



class Account(
        private var amount: Double,
        private var labelAccount: String,
        private var sheets: List<Sheet>
){

    fun sheets(): List<Sheet>{
        return sheets.distinct()
    }

    fun label(): String{
        return labelAccount
    }
    operator fun Account.plusAssign(earned: Double){
        this.amount += earned
    }
    operator fun Account.minusAssign(loss: Double){
        amount -= loss
    }
    fun transaction(delta: Double, otherAccount: Account, isEntry: Boolean){
        if(isEntry){
            this -= delta
            otherAccount += delta
        } else {
            this += delta
            otherAccount -= delta
        }
    }
}
