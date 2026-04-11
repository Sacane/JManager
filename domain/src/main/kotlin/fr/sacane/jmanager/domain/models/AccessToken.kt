package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.Env
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.*


val roleUser = listOf(Role.USER)
val roleAdmin = listOf(Role.USER, Role.ADMIN)


data class AccessToken(
    val userId: UserId,
    val userName: String,
    val tokenValue: String,
    var tokenExpirationDate: LocalDateTime = now().plusHours(1),
    val refreshToken: UUID? = UUID.randomUUID(),
    var refreshTokenLifetime: LocalDateTime = now().plusDays(1),
    val roles: Set<Role> = setOf(Role.USER)
){
    fun isExpired(): Boolean{
        return tokenExpirationDate.isBefore(now())
    }

    fun updateLifetime() {
        tokenExpirationDate = now().plusMinutes(Env.TOKEN_LIFETIME_IN_MINUTES)
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
    val token: String,
    val refreshToken: UUID? = null,
)