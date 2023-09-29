package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.Response.Companion.forbidden
import fr.sacane.jmanager.domain.models.Response.Companion.ok
import fr.sacane.jmanager.domain.models.Response.Companion.timeout
import fr.sacane.jmanager.domain.models.Response.Companion.unauthorized
import java.util.*


@DomainService
class SessionManager {
    private val userSession: MutableMap<UserId, AccessToken> = mutableMapOf()
    fun addSession(userId: UserId, session: AccessToken) {
        userSession[userId] = session
    }
    infix fun getSession(userId: UserId) = userSession[userId]
    fun <T> authenticate(
        userId: UserId,
        token: UUID,
        requiredRoles: Array<Role> = arrayOf(Role.USER, Role.ADMIN),
        block: (UserId) -> Response<T>
    ): Response<T> {
        val session = getSession(userId) ?: return unauthorized("L'utilisateur n'est pas connecté à la session")
        if(!requiredRoles.contains(session.role)) return unauthorized("L'utilisateur n'a pas le rôle adéquat pour accéder à cette requête")
        if(session.isExpired()) return timeout("La session a expiré")
        if(session.tokenValue != token) return unauthorized("Le token est invalide")
        session.updateLifetime()
        session.updateTokenLifetime()
        return block(userId)
    }
    fun tryRefresh(id: UserId, refreshToken: UUID): Response<AccessToken> {
        val session = getSession(id) ?: return unauthorized("L'utilisateur n'est pas connecté")
        if(session.refreshToken != refreshToken || session.isRefreshTokenExpired()) {
            return forbidden("Vous le refresh token est incorrect, impossible de renvoyer de token valide")
        }
        val regeneratedToken = generateToken(session.role)
        userSession[id] = regeneratedToken
        return ok(regeneratedToken)
    }
    infix fun removeSession(userId: UserId) {
        val session = userSession[userId] ?: return
        if(session.isExpired()) {
            userSession.remove(userId)
        }
    }
}

//object Session {
//    private val userSession: MutableMap<UserId, AccessToken> = mutableMapOf()
//
//    fun addSession(userId: UserId, session: AccessToken) {
//        userSession[userId] = session
//    }
//
//    infix fun getSession(userId: UserId) = userSession[userId]
//    fun <T> authenticate(
//        userId: UserId,
//        token: UUID,
//        requiredRoles: Array<Role> = arrayOf(Role.USER, Role.ADMIN),
//        block: (UserId) -> Response<T>
//    ): Response<T> {
//        val session = getSession(userId) ?: return unauthorized("L'utilisateur n'est pas connecté")
//        if(!requiredRoles.contains(session.role)) return unauthorized("L'utilisateur n'a pas le rôle adéquat pour accéder à cette requête")
//        if(session.isExpired()) return timeout("La session a expiré")
//        if(session.tokenValue != token) return unauthorized("Le token est invalide")
//        session.updateLifetime()
//        session.updateTokenLifetime()
//        return block(userId)
//    }
//
//    fun tryRefresh(id: UserId, refreshToken: UUID): Response<AccessToken> {
//        val session = getSession(id) ?: return unauthorized("L'utilisateur n'est pas connecté")
//        if(session.refreshToken != refreshToken || session.isRefreshTokenExpired()) {
//            return forbidden("Vous le refresh token est incorrect, impossible de renvoyer de token valide")
//        }
//        val regeneratedToken = generateToken(session.role)
//        userSession[id] = regeneratedToken
//        return ok(regeneratedToken)
//    }
//
//    fun removeSession(userId: UserId) {
//        userSession.remove(userId)
//    }
//}