package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*

@Port(Side.DATASOURCE)
interface LoginRegisterManager {
    fun login(userPseudonym: String, password: Password): UserToken?
    fun logout(userId: UserId, token: Token): Token?
    fun refresh(userId: UserId, token: Token): UserToken?
    fun tokenBy(userId: UserId): UserToken?
    fun generateToken(user: User): Token?
    fun deleteToken(userId: UserId)

    fun <T> authenticate(userId: UserId, token: Token, action: (user: User) -> Response<T>): Response<T> {
        val userToken = tokenBy(userId) ?: return Response.notFound("Il n'existe pas d'utilisateur avec cette ID : $userId")
        if(userToken.token.isExpired()) {
            return Response.timeout("La session à expiré")
        }
        if(userToken.token != token) {
            return Response.invalid("Le token n'est pas valide")
        }
        return action(userToken.user)
    }
}