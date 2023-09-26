package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.Response.Companion.forbidden
import fr.sacane.jmanager.domain.models.Response.Companion.invalid
import fr.sacane.jmanager.domain.models.Response.Companion.ok
import fr.sacane.jmanager.domain.models.Response.Companion.timeout
import fr.sacane.jmanager.domain.models.Response.Companion.unauthorized
import java.util.*

object Session {
    private val connectedUsers = mutableListOf<User>()
    private val userSession: MutableMap<UserId, AccessToken> = mutableMapOf()

    internal fun addSession(userId: UserId, session: AccessToken) {
        userSession[userId] = session
    }

    internal fun addUser(user: User) {
        connectedUsers.add(user)
        println(connectedUsers)
    }
    internal infix fun removeSession(userId: UserId): AccessToken? {
        connectedUsers.removeIf { it.id == userId }
        return userSession.remove(userId)
    }
    infix fun getSession(userId: UserId) = userSession[userId]
    private infix fun getUser(userId: UserId): User? = connectedUsers.find { it.id == userId }
    fun <T> authenticate(
        userId: UserId,
        token: UUID,
        requiredRoles: Array<Role> = arrayOf(Role.USER, Role.ADMIN),
        block: User.() -> Response<T>
    ): Response<T> {
        val session = getSession(userId) ?: return unauthorized("L'utilisateur n'est pas connecté")
        if(!requiredRoles.contains(session.role)) return unauthorized("L'utilisateur n'a pas le rôle adéquat pour accéder à cette requête")
        if(session.isExpired()) return timeout("La session a expiré")
        if(session.tokenValue != token) return unauthorized("Le token est invalide")
        session.updateLifetime()
        session.updateTokenLifetime()
        val currentUser = getUser(userId) ?: return invalid("L'utilisateur n'est pas connecté")
        return block.invoke(currentUser)
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
}