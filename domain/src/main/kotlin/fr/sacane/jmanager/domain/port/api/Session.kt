package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.Response.Companion.forbidden
import fr.sacane.jmanager.domain.models.Response.Companion.ok
import fr.sacane.jmanager.domain.models.Response.Companion.timeout
import fr.sacane.jmanager.domain.models.Response.Companion.unauthorized
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId
import java.util.*
import java.util.logging.Logger


@DomainService
class SessionManager {
    private val logger: Logger = Logger.getLogger(SessionManager::class.java.name)
    companion object {
        const val PURGE_DELAY = 1_800_000L // 30 minutes in milliseconds
    }

    private val lock: Any = Any()
    private val userSession: MutableMap<UserId, MutableSet<AccessToken>> = mutableMapOf()

    fun addSession(userId: UserId, session: AccessToken) = synchronized(lock){
        val sessions = userSession.computeIfAbsent(userId) { mutableSetOf() }
        sessions.add(session)
    }
    private fun getSession(userId: UserId, token: UUID): AccessToken? = userSession[userId]?.first { token == it.tokenValue || token == it.refreshToken }
    fun <T> authenticate(
        userId: UserId,
        token: UUID,
        requiredRoles: Array<Role> = arrayOf(Role.USER, Role.ADMIN),
        block: (UserId) -> Response<T>
    ): Response<T> {
        synchronized(lock) {
            val session = getSession(userId, token) ?: return unauthorized("L'utilisateur n'est pas connecté à la session")

            if (!requiredRoles.contains(session.role)) return unauthorized("L'utilisateur n'a pas le rôle adéquat pour accéder à cette requête")
            if (session.isExpired()) return timeout("La session a expiré")
            if (session.tokenValue != token) return unauthorized("Le token est invalide")
            session.updateLifetime()
            session.updateTokenLifetime()
        }
        return block(userId)
    }
    fun tryRefresh(id: UserId, refreshToken: UUID): Response<AccessToken> = synchronized(lock) {
        logger.info("Trying to refresh session user $id")
        val session = getSession(id, refreshToken) ?: return unauthorized("L'utilisateur n'est pas connecté")
        if (session.refreshToken != refreshToken || session.isRefreshTokenExpired()) {
            return forbidden("Le refresh token est incorrect, impossible de renvoyer de token valide")
        }
        session.updateLifetime()
        session.updateTokenLifetime()
        return ok(session)
    }
    infix fun removeSession(userId: UserId) = synchronized(lock) {
        userSession[userId]?.clear()
    }
    fun removeSession(userId: UserId, token: UUID) = synchronized(lock){
        userSession[userId]?.removeIf{it.tokenValue == token}
    }
    fun purgeExpiredToken() = synchronized(lock) {
        var counter = 0
        logger.info("Start purge expired tokens")
        userSession.values.forEach {
            val result = it.removeIf {token ->
                token.isExpired()
            }
            if(result) counter++
        }
        userSession.entries.removeIf { (id, set) -> set.isEmpty() }
        logger.info("Purge done, erased $counter tokens")
    }
}
