package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.Env
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.*
enum class Role {
    ADMIN, USER
}
data class Token(
    val value: UUID,
    var tokenLifeTime: LocalDateTime? = null,
    val refreshToken: UUID? = UUID.randomUUID(),
    var refreshTokenLifetime: LocalDateTime? = null,
    val role: Role = Role.USER
){
    fun isExpired(): Boolean{
        check(tokenLifeTime != null){
            "trying to check a token that has been created without lastRefresh indication"
        }
        return tokenLifeTime!!.isBefore(now())
    }
    fun isRefreshTokenExpired(): Boolean {
        check(refreshTokenLifetime != null) {
            "Impossible de vérifier la validité d'un refresh token inexistante"
        }
        return refreshTokenLifetime!!.isBefore(now())
    }

    fun updateLifetime() {
        check(tokenLifeTime != null) {
            "Impossible de mettre à jour le temps de vie d'un token inexistant"
        }
        tokenLifeTime = tokenLifeTime!!.plusHours(Env.TOKEN_LIFETIME_IN_HOURS)
    }
    fun updateTokenLifetime() {
        check(refreshTokenLifetime != null) {
            "Impossible de mettre à jour le temps de vie d'un token inexistant"
        }
        refreshTokenLifetime = refreshTokenLifetime!!.plusDays(Env.REFRESH_TOKEN_LIFETIME_IN_DAYS)
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