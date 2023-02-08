package fr.sacane.jmanager.domain.models

import java.time.LocalDateTime
import java.util.*
data class Token(
    val id: UUID,
    val lastRefresh: LocalDateTime?,
    val refreshToken: UUID
)
enum class TicketState{
    OK,
    TIMEOUT,
    INVALID;
    fun isSuccess(): Boolean{
        return this == OK
    }
    fun isFailure(): Boolean{
        return this != OK
    }
}
class Ticket(
    val user: User?,
    val token: Token?
){
    fun checkForIdentity(token: Token): User?{
        require(this.token != null && this.user != null){
            "Forbidden check for invalid ticket"
        }
        return if(this.token.id == token.id){
            this.user
        } else null
    }
}