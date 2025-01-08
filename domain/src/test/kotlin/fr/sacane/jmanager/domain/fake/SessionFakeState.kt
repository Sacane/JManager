package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.BiState
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.Result.Companion.unauthorized
import fr.sacane.jmanager.domain.utils.forbidden
import fr.sacane.jmanager.domain.utils.success
import fr.sacane.jmanager.domain.utils.timeout
import java.util.*

data class UserSessionEntry (
    val userId: UserId,
    val accessToken: AccessToken
)

class SessionFakeState: BiState<List<UserSessionEntry>, List<AccessToken>>, SessionManager {
    private val lock: Any = Any()
    private val userSession: MutableMap<UserId, MutableSet<AccessToken>> = mutableMapOf()

    override fun getStates(): List<AccessToken> {
        return userSession.values.flatten()
    }

    override fun clear() {
        userSession.clear()
    }

    override fun init(initialState: List<UserSessionEntry>) {
        initialState.forEach {
            userSession[it.userId] = mutableSetOf(it.accessToken)
        }
    }

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
        block: (UserId) -> Result<T>
    ): Result<T> {
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
    override fun tryRefresh(id: UserId, refreshToken: UUID): Result<AccessToken> = synchronized(lock) {
        val session = getSession(id, refreshToken) ?: return unauthorized("L'utilisateur n'est pas connecté")
        if (session.refreshToken != refreshToken || session.isRefreshTokenExpired()) {
            return forbidden("Le refresh token est incorrect, impossible de renvoyer de token valide")
        }
        session.updateLifetime()
        session.updateTokenLifetime()
        return success(session)
    }

    override fun removeSession(userId: UserId, token: UUID): Unit = synchronized(lock){
        userSession[userId]?.removeIf{it.tokenValue == token}
    }
    override fun purgeExpiredToken(): Unit = synchronized(lock) {
        var counter = 0
        userSession.values.forEach {
            val result = it.removeIf { token ->
                token.isExpired()
            }
            if(result) counter++
        }
        userSession.entries.removeIf { (_, set) -> set.isEmpty() }
    }
}