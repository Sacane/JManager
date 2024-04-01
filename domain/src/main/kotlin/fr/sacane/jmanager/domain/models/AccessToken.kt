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
    var tokenExpirationDate: LocalDateTime = now().plusHours(1),
    val refreshToken: UUID? = UUID.randomUUID(),
    var refreshTokenLifetime: LocalDateTime = now().plusDays(1),
    val role: Role = Role.USER
){
    fun isExpired(): Boolean{
        return tokenExpirationDate.isBefore(now())
    }
    fun isRefreshTokenExpired(): Boolean {
        return refreshTokenLifetime.isBefore(now())
    }

    fun updateLifetime() {
        tokenExpirationDate = tokenExpirationDate.plusHours(Env.TOKEN_LIFETIME_IN_HOURS)
    }
    fun updateTokenLifetime() {
        refreshTokenLifetime = refreshTokenLifetime.plusDays(Env.REFRESH_TOKEN_LIFETIME_IN_DAYS)
    }

    override fun equals(other: Any?): Boolean {
        return other is AccessToken && other.tokenValue == tokenValue
    }

    override fun hashCode(): Int {
        return tokenValue.hashCode()
    }
}
data class UserToken(
    val user: MinimalUserRepresentation,
    val token: AccessToken
)

fun generateToken(role: Role = Role.USER)
: AccessToken = AccessToken(
    UUID.randomUUID(),
    now().plusHours(Env.TOKEN_LIFETIME_IN_HOURS),
    UUID.randomUUID(),
    now().plusDays(Env.REFRESH_TOKEN_LIFETIME_IN_DAYS), role
)
