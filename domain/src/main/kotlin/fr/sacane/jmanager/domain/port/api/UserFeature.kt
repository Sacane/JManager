package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.Hasher
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.*
import java.util.*
import java.util.logging.Logger

@Port(Side.APPLICATION)
sealed interface UserFeature {
    fun login(pseudonym: String, userPassword: String): Result<UserToken>
    fun logout(userId: UserId, token: UUID): Result<Nothing>
    fun register(username: String, password: String, confirmPassword: String): Result<User>
    fun tryRefresh(userId: UserId, refreshToken: UUID): Result<Pair<User, AccessToken>>
}


@DomainService
class UserFeatureImpl(
    private val userRepository: UserRepository,
    private val session: SessionManager,
    private val hasher: Hasher
): UserFeature{

    companion object{
        private val LOGGER = Logger.getLogger(UserFeatureImpl::class.java.name)
    }
    override fun login(pseudonym: String, userPassword: String): Result<UserToken> {
        val userWithPassword = userRepository.findByPseudonymWithEncodedPassword(pseudonym)
            ?: return failure(ResultState.NOT_FOUND, "L'utilisateur $pseudonym n'existe pas")
        LOGGER.info("LOGIN request for user ${userWithPassword.user.id}")
        val user = userWithPassword.user
        if(hasher.verify(userPassword, userWithPassword.password)) {
            LOGGER.info("User $pseudonym logged")
            val accessToken = generateToken(Role.USER)
            session.addSession(user.id, accessToken)
            return success(user.withToken(accessToken))
        }
        LOGGER.warning("Failed to log user $pseudonym")
        return failure(ResultState.USER_UNAUTHORIZED, "Le pseudonyme ou le mot de passe est incorrect")
    }

    override fun logout(userId: UserId, token: UUID)
    : Result<Nothing> = session.authenticate(userId, token) {
        session.removeSession(userId, token)
        success()
    }

    override fun register(username: String, password: String, confirmPassword: String): Result<User> {
        if(password != confirmPassword) return failure(ResultState.PASSWORD_NOT_MATCH, "Les mots de passes ne correspondent pas")
        val hashedPassword = hasher.hash(password)
        val userResult = userRepository.register(username, hashedPassword) ?: return invalid("Une erreur est survenue")
        return success(userResult)
    }

    override fun tryRefresh(userId: UserId, refreshToken: UUID): Result<Pair<User, AccessToken>> {
        val user = userRepository.findUserById(userId) ?: return failure(ResultState.USER_NOT_FOUND,"L'utilisateur $userId n'est pas enregistré en base")
        return session.tryRefresh(userId, refreshToken)
            .mapBoth({value -> success(Pair(user, value ?: AccessToken(UUID.randomUUID())))}) {
                failure(ResultState.UNAUTHORIZED, it.first)
            } ?: throw IllegalStateException("Invalid empty response has been given through this operation")
    }

}
