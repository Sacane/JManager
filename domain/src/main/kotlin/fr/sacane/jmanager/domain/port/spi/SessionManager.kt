package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.Result.Companion.unauthorized
import fr.sacane.jmanager.domain.utils.timeout
import java.util.logging.Logger

interface SessionManager{
    fun addSession(userId: UserId, session: AccessToken)
    fun <T> authenticate(
        token: String,
        requiredRoles: Array<Role> = arrayOf(Role.USER, Role.ADMIN),
        block: (UserId) -> Result<T>
    ): Result<T>
    fun removeSession(userId: UserId, token: String)
    fun purgeExpiredToken()
}

@DomainService
class InMemorySessionManager(private val tokenGenerator: TokenGenerator) : SessionManager {
    private val logger: Logger = Logger.getLogger(InMemorySessionManager::class.java.name)
    companion object {
        const val PURGE_DELAY = 1_800_000L // 30 minutes in milliseconds
    }

    private val lock: Any = Any()
    private val userSession: MutableMap<Long, MutableSet<AccessToken>> = mutableMapOf()

    override fun addSession(userId: UserId, session: AccessToken): Unit = synchronized(lock){
        val sessions = userSession.computeIfAbsent(userId.value!!) { mutableSetOf() }
        sessions.add(session)
    }
    private fun getSession(userId: UserId, token: String): AccessToken? = synchronized(lock) {
        return try {
            userSession[userId.value]?.first { token == it.tokenValue }
        }catch (noSuchElementEx: NoSuchElementException){
            null
        }
    }
    override fun <T> authenticate(
        token: String,
        requiredRoles: Array<Role>,
        block: (UserId) -> Result<T>
    ): Result<T> {
        val accessToken = synchronized(lock) {
            val decodedToken = tokenGenerator.readToken(token) ?: return unauthorized("Le token est invalide, une erreur est survenu à la lecture")
            val session = getSession(decodedToken.userId, decodedToken.tokenValue) ?: return unauthorized("L'utilisateur n'est pas connecté à la session")
            if (!requiredRoles.contains(session.role)) return unauthorized("L'utilisateur n'a pas le rôle adéquat pour accéder à cette requête")
            if (session.isExpired()) return timeout("La session a expiré")
            if (session.tokenValue != token) return unauthorized("Le token est invalide")
            session.updateLifetime()
            session.updateTokenLifetime()
            decodedToken
        }
        return block(accessToken.userId)
    }

    override fun removeSession(userId: UserId, token: String): Unit = synchronized(lock){
        userSession[userId.value]?.removeIf{it.tokenValue == token}
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
