package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.Response.Companion.invalid
import fr.sacane.jmanager.domain.models.Response.Companion.notFound
import fr.sacane.jmanager.domain.models.Response.Companion.timeout


@Port(Side.DATASOURCE)
interface UserTransaction {
    fun findById(userId: UserId): UserToken?
    fun findUserById(userId: UserId): User?
    fun checkUser(pseudonym: String, pwd: Password): UserToken?
    fun findByPseudonym(pseudonym: String): User?
    fun create(user: User): User?
    fun register(user: User): User?
    fun getUserToken(userId: UserId): Token?
    fun upsert(user: User): User?

    fun <T> authenticate(userId: UserId, token: Token, action: () -> Response<T>): Response<T> {
        val userToken = getUserToken(userId) ?: return notFound("Il n'existe pas d'utilisateur avec cette ID : $userId")
        if(userToken.isExpired()) {
            return timeout("La session à expiré")
        }
        if(userToken != token) {
            return invalid("Le token n'est pas valide")
        }
        val t = findById(userId) ?: return notFound("Il n'existe pas d'utilisateur avec cette ID : $userId")
        t.checkForIdentity(userToken) ?: return invalid("Le token utilisé par l'utilisateur est incorrect")
        return action()
    }
}