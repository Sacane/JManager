package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*

@Port(Side.DATASOURCE)
interface SessionRepository {
    fun login(userPseudonym: String, password: Password): UserToken?
    fun logout(userId: UserId, token: AccessToken): AccessToken?
    fun refresh(userId: UserId, token: AccessToken): UserToken?
    fun tokenBy(userId: UserId): UserToken?
    fun generateToken(user: User): AccessToken?
    fun deleteToken(userId: UserId)
    fun deleteTokens(tokens: List<AccessToken>)
    fun refreshTokenLifetime(userID: UserId, refreshLifeTime: Boolean = false): AccessToken?
    fun save(token: AccessToken): AccessToken?
    fun tokens(): List<AccessToken>

    fun <T> authenticate(userId: UserId, token: AccessToken, action: User.() -> Response<T>): Response<T> {
        val userToken = tokenBy(userId) ?: return Response.notFound("Il n'existe pas d'utilisateur avec cette ID : $userId")
//        if(userToken.token.isExpired()) {
//            return Response.timeout("La session à expiré")
//        }
        if(userToken.token != token) {
            return Response.invalid("Le token n'est pas valide")
        }
        refreshTokenLifetime(userId)
        return action(userToken.user)
    }
}