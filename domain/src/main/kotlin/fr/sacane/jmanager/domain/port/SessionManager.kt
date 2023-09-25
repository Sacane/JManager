package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.Response.Companion.forbidden
import fr.sacane.jmanager.domain.models.Response.Companion.invalid
import fr.sacane.jmanager.domain.models.Response.Companion.ok
import fr.sacane.jmanager.domain.models.Response.Companion.timeout
import fr.sacane.jmanager.domain.models.Response.Companion.unauthorized
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.util.UUID
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Secure(val role: Array<Role>)

/**
 * Map uuid -> AccessToken
 *
 * Login -> check -> generation uuid/accessToken in map if success
 * else return anauthorized
 * each request -> {
 *  - check header role ?: unauthorized
 *  - check user token if exists return this token ?: unauthorized
 *  - check lifetime token ?: timeout {
 *  user resend refresh token :
 *  - Check refresh Token exists ?: OK(option: redirect['/login']) + clear
 *  - Check refresh lifetime is OK ?: redirect['/login']
 *  } if success => refresh new Token ()
 * }
 *
 * @Scheduled = each day reset timeout token and refresh token

@DomainService
class SessionManager(
    private val userRepository: UserRepository
) {

    fun login(username: String, password: Password): Response<AccessToken> {
        val user = userRepository.findByPseudonym(username) ?: return unauthorized("L'utilisateur $username n'existe pas")
        if(user.password.matchWith(password)) {
            val accessToken = generateToken(Role.USER)
            Session.addSession(user.id, accessToken)
            return ok(accessToken)
        }
        return unauthorized("Le mot de passe est incorrect")
    }
    fun logout(userId: UserId): Response<Nothing> {
        val user = userRepository.findById(userId) ?: return unauthorized("L'utilisateur n'existe pas")
        Session.removeSession(userId)
        return ok()
    }
}*/

object Session {
    private val connectedUsers = mutableListOf<User>()
    private val userSession: MutableMap<UserId, AccessToken> = mutableMapOf()

    internal fun addSession(userId: UserId, session: AccessToken) {
        userSession[userId] = session
        connectedUsers.removeIf { it.id == userId }
    }

    internal fun addUser(user: User) {
        connectedUsers.add(user)
    }
    internal infix fun removeSession(userId: UserId): AccessToken? {
        return userSession.remove(userId)
    }
    infix fun getSession(userId: UserId) = userSession[userId]
    infix fun getUser(userId: UserId): User? = connectedUsers.find { it.id == userId }
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