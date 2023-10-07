package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.SessionRepository
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.lang.IllegalStateException
import java.util.UUID
import java.util.logging.Logger

@Port(Side.API)
sealed interface SessionFeature {
    fun login(pseudonym: String, userPassword: Password): Response<UserToken>
    fun logout(userId: UserId, token: UUID): Response<Nothing>
    fun register(username: String, email: String, password: String, confirmPassword: String): Response<User>
    fun tryRefresh(userId: UserId, refreshToken: UUID): Response<Pair<User, AccessToken>>
}


@DomainService
class SessionFeatureImpl(
    private val userRepository: UserRepository,
    private val session: SessionManager
): SessionFeature{

    companion object{
        private val LOGGER = Logger.getLogger(SessionRepository::class.java.name)
    }
    override fun login(pseudonym: String, userPassword: Password): Response<UserToken> {
        LOGGER.info("Trying to login user : $pseudonym")
        val user = userRepository.findByPseudonym(pseudonym) ?: return Response.notFound("L'utilisateur $pseudonym n'existe pas")
        if(userPassword.matchWith(user.password)) {
            LOGGER.info("User $pseudonym logged")
            val accessToken = generateToken(Role.USER)
            session.addSession(user.id, accessToken)
            return Response.ok(user.withToken(accessToken))
        }
        LOGGER.warning("Failed to log user $pseudonym")
        return Response.forbidden("Le pseudonyme ou le mot de passe est incorrect")
    }

    override fun logout(userId: UserId, token: UUID)
    : Response<Nothing>  = session.authenticate(userId, token) {
        session.removeSession(userId)
        Response.ok()
    }

    override fun register(username: String, email: String, password: String, confirmPassword: String): Response<User> {
        if(password != confirmPassword) return Response.invalid("Les mots de passes ne correspondent pas")
        val user = User(
            username = username,
            email = email,
            password= Password(password)
        )
        val userResponse = userRepository.register(user) ?: return Response.invalid()
        return Response.ok(userResponse)
    }

    override fun tryRefresh(userId: UserId, refreshToken: UUID): Response<Pair<User, AccessToken>> {
        val user = userRepository.findUserById(userId) ?: return Response.notFound("L'utilisateur n'est pas enregistré en base")
        return session.tryRefresh(userId, refreshToken)
            .mapBoth({value -> Response.ok(Pair(user, value ?: AccessToken(UUID.randomUUID())))}) {
                Response.invalid(it.first)
            } ?: throw IllegalStateException("Invalid empty response has been given through this operation")
    }

}