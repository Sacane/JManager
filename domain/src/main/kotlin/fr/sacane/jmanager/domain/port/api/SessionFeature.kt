package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.Session
import fr.sacane.jmanager.domain.port.spi.SessionRepository
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.lang.IllegalStateException
import java.util.UUID
import java.util.logging.Logger

@Port(Side.API)
sealed interface LoginFeature {
    fun login(pseudonym: String, userPassword: Password): Response<UserToken>
    fun logout(userId: UserId, token: UUID): Response<Nothing>
    fun register(user: User): Response<User>
    fun tryRefresh(userId: UserId, refreshToken: UUID): Response<Pair<User, AccessToken>>
}

@DomainService
class LoginManager(
    private val userRepository: UserRepository
): LoginFeature{

    companion object{
        private val LOGGER = Logger.getLogger(SessionRepository::class.java.name)
    }
    override fun login(pseudonym: String, userPassword: Password): Response<UserToken> {
        LOGGER.info("Trying to login user : $pseudonym")
        val user = userRepository.findByPseudonym(pseudonym) ?: return Response.notFound("L'utilisateur $pseudonym n'existe pas")
        if(userPassword.matchWith(user.password)) {
            LOGGER.info("User $pseudonym logged")
            val accessToken = generateToken(Role.USER)
            Session.addSession(user.id, accessToken)
            Session.addUser(user)
            return Response.ok(user.withToken(accessToken))
        }
        LOGGER.warning("Failed to log user $pseudonym")
        return Response.forbidden("Le pseudonyme ou le mot de passe est incorrect")
    }

    override fun logout(userId: UserId, token: UUID)
    : Response<Nothing>  = Session.authenticate(userId, token) {
        Session.removeSession(userId) ?: return@authenticate Response.invalid("La session n'a pu être supprimé")
        Response.ok()
    }


    override fun register(user: User): Response<User> {
        val userResponse = userRepository.register(user) ?: return Response.invalid()
        return Response.ok(userResponse)
    }

    override fun tryRefresh(userId: UserId, refreshToken: UUID): Response<Pair<User, AccessToken>> {
        val user = userRepository.findUserById(userId) ?: return Response.notFound("L'utilisateur n'est pas enregistré en base")
        val session = Session.getSession(userId) ?: return Response.unauthorized("L'utilisateur n'a pas de session active")
        if(session.refreshToken != refreshToken) return Response.forbidden("Les refresh tokens ne correspondent pas, l'utilisateur doit être déconnecté")
        return Session.tryRefresh(userId, refreshToken)
            .mapBoth({value -> Response.ok(Pair(user, value ?: AccessToken(UUID.randomUUID())))}) {
                Response.invalid(it.first)
            } ?: throw IllegalStateException("Invalid empty response has been given through this operation")
    }

}