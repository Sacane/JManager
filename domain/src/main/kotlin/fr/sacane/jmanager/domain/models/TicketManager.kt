package fr.sacane.jmanager.domain.models

import java.time.LocalDateTime
import java.util.*
data class Token(
    val id: UUID,
    val lastRefresh: LocalDateTime?,
    val refreshToken: UUID?
){
    fun isExpired(time: LocalDateTime): Boolean{
        require(lastRefresh != null){
            "trying to check a token that has been created without lastRefresh indication"
        }
        return lastRefresh.isBefore(time)
    }
    fun isExpired(): Boolean{
        require(lastRefresh != null){
            "trying to check a token that has been created without lastRefresh indication"
        }
        return lastRefresh.isBefore(LocalDateTime.now())
    }
}
class Ticket(
    val user: User,
    val token: Token
){
    fun checkForIdentity(token: Token): User?{
        return if(this.token.id == token.id || this.token.lastRefresh!!.isAfter(LocalDateTime.now())){
            this.user
        } else null
    }
}