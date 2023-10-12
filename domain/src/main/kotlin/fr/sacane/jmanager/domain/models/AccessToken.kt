package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.Env
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.*
enum class Role {
    ADMIN, USER
}

val roleUser = arrayOf(Role.USER)
val roleAdmin = arrayOf(Role.ADMIN)


class AccessToken(
    val tokenValue: UUID,
    var tokenExpirationDate: LocalDateTime? = null,
    val refreshToken: UUID? = UUID.randomUUID(),
    var refreshTokenLifetime: LocalDateTime? = null,
    val role: Role = Role.USER
){
    fun isExpired(): Boolean{
        check(tokenExpirationDate != null){
            "trying to check a token that has been created without lastRefresh indication"
        }
        return tokenExpirationDate!!.isBefore(now())
    }
    fun isRefreshTokenExpired(): Boolean {
        check(refreshTokenLifetime != null) {
            "Impossible de vérifier la validité d'un refresh token inexistante"
        }
        return refreshTokenLifetime!!.isBefore(now())
    }

    fun updateLifetime() {
        check(tokenExpirationDate != null) {
            "Impossible de mettre à jour le temps de vie d'un token inexistant"
        }
        tokenExpirationDate = tokenExpirationDate!!.plusHours(Env.TOKEN_LIFETIME_IN_HOURS)
    }
    fun updateTokenLifetime() {
        check(refreshTokenLifetime != null) {
            "Impossible de mettre à jour le temps de vie d'un token inexistant"
        }
        refreshTokenLifetime = refreshTokenLifetime!!.plusDays(Env.REFRESH_TOKEN_LIFETIME_IN_DAYS)
    }

    override fun equals(other: Any?): Boolean {
        return other is AccessToken && other.tokenValue == tokenValue
    }

    override fun hashCode(): Int {
        return tokenValue.hashCode()
    }
}
data class UserToken(
    val user: User,
    val token: AccessToken
){
    fun checkForIdentity(token: AccessToken): User?{
        return if(this.token.tokenValue == token.tokenValue || this.token.tokenExpirationDate!!.isAfter(now())){
            this.user
        } else null
    }
}

fun generateToken(role: Role = Role.USER)
: AccessToken = AccessToken(
    UUID.randomUUID(),
    now().plusHours(Env.TOKEN_LIFETIME_IN_HOURS),
    UUID.randomUUID(),
    now().plusDays(Env.REFRESH_TOKEN_LIFETIME_IN_DAYS), role
)
