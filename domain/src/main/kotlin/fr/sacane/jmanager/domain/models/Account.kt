package fr.sacane.jmanager.domain.models

class Account(
        private var id: Long?,
        private var amount: Double,
        private val labelAccount: String,
        val sheets: MutableList<Sheet>?
){

    override fun equals(other: Any?): Boolean = (other is Account) && labelAccount == other.label()
    fun sheets(): List<Sheet>?{
        if(sheets == null) return null
        return if(sheets.isEmpty()) null else sheets.toList()
    }
    fun amount(): Double = amount

    fun label(): String{
        return labelAccount
    }
    fun id(): Long{
        return id!!
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
}
