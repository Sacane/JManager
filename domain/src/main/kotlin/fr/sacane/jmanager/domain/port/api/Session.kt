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

interface SessionManager{
    fun addSession(userId: UserId, session: AccessToken)
    fun <T> authenticate(
        userId: UserId,
        token: UUID,
        requiredRoles: Array<Role> = arrayOf(Role.USER, Role.ADMIN),
        block: (UserId) -> Response<T>
    ): Response<T>
    fun tryRefresh(id: UserId, refreshToken: UUID): Response<AccessToken>
    fun removeSession(userId: UserId, token: UUID)
    fun purgeExpiredToken()
}

@DomainService
class InMemorySessionManager : SessionManager {
    private val logger: Logger = Logger.getLogger(InMemorySessionManager::class.java.name)
    companion object {
        const val PURGE_DELAY = 1_800_000L // 30 minutes in milliseconds
    }

    private val lock: Any = Any()
    private val userSession: MutableMap<UserId, MutableSet<AccessToken>> = mutableMapOf()

    override fun addSession(userId: UserId, session: AccessToken): Unit = synchronized(lock){
        val sessions = userSession.computeIfAbsent(userId) { mutableSetOf() }
        sessions.add(session)
    }
    private fun getSession(userId: UserId, token: UUID): AccessToken? {
        return try {
            userSession[userId]?.first { token == it.tokenValue || token == it.refreshToken }
        }catch (noSuchElementEx: NoSuchElementException){
            null
        }
    }
    override fun <T> authenticate(
        userId: UserId,
        token: UUID,
        requiredRoles: Array<Role>,
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
    override fun tryRefresh(id: UserId, refreshToken: UUID): Response<AccessToken> = synchronized(lock) {
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
    override fun removeSession(userId: UserId, token: UUID): Unit = synchronized(lock){
        userSession[userId]?.removeIf{it.tokenValue == token}
    }
    override fun purgeExpiredToken() = synchronized(lock) {
        var counter = 0
        logger.info("Start purge expired tokens of => ${userSession.count()}")
        userSession.values.forEach {
            val result = it.removeIf { token ->
                token.isExpired()
            }
            if(result) counter++
        }
        userSession.entries.removeIf { (_, set) -> set.isEmpty() }
        logger.info("Purge done, erased $counter tokens")
    }
}
