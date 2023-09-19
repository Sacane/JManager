package fr.sacane.jmanager.domain.models

import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.*
data class Token(
    val value: UUID,
    val tokenLifeTime: LocalDateTime? = null,
    val refreshToken: UUID? = UUID.randomUUID(),
    val refreshTokenLifetime: LocalDateTime? = null
){
    fun isExpired(): Boolean{
        check(tokenLifeTime != null){
            "trying to check a token that has been created without lastRefresh indication"
        }
        return tokenLifeTime.isBefore(now())
    }

    override fun equals(other: Any?): Boolean {
        return other is Token && other.value == value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
data class UserToken(
    val user: User,
    val token: Token
){
    fun checkForIdentity(token: Token): User?{
        return if(this.token.value == token.value || this.token.tokenLifeTime!!.isAfter(now())){
            this.user
        } else null
    }
}