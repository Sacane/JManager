package fr.sacane.jmanager.domain.model

data class AccessTicket(
    val user: User,
    val hasAccess: Boolean
)
//enum class Action{
//    TRANSACTION, ACCESS
//}

//internal fun Action.byName(name: String): Action{
//    return when(name){
//        "transaction", "Transaction", "TRANSACTION" -> Action.TRANSACTION
//        "Access", "Check", "access" -> Action.ACCESS
//        else -> throw IllegalArgumentException()
//    }
//}
