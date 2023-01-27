package fr.sacane.jmanager.domain.model

import java.time.LocalDateTime
import java.util.*

data class Token(val id: UUID, val lastRefresh: LocalDateTime)

data class AccessTicket(
    val user: User,
    val hasAccess: Boolean,
    val token: Token?
){
    fun isValidate(): Boolean{
        return !(!hasAccess && token != null)
    }
}
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
