package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserToken
import fr.sacane.jmanager.domain.port.spi.Hasher
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.*
import java.util.logging.Logger

@Port(Side.APPLICATION)
/**
 * Application port: UserFeature
 *
 * High-level API for user authentication and user management use-cases exposed by the domain.
 * Implementations are responsible for handling authentication and returning domain Result<T>
 * objects signifying success or failure states.
 */
sealed interface UserFeature {
    /**
     * Authenticate a user with the given pseudonym and password.
     *
     * @param pseudonym The user's pseudonym or username used to identify the account.
     * @param userPassword The plain-text password to verify against the stored credentials.
     * @return Result containing a UserToken on success (including an access token), or an
     *         appropriate failure state (e.g. NOT_FOUND, USER_UNAUTHORIZED).
     */
    fun login(pseudonym: String, userPassword: String): Result<UserToken>

    /**
     * Invalidate a session identified by the provided token.
     *
     * @param token The session/access token to invalidate.
     * @return Result with no value on success, or an error if the token is invalid.
     */
    fun logout(token: String): Result<Nothing>

    /**
     * Register a new user account.
     *
     * @param username Desired username for the new account.
     * @param password Desired password for the new account (will be hashed by the domain).
     * @param confirmPassword Confirmation of the password; must match `password`.
     * @return Result containing the created User on success, or a failure state when
     *         validation or persistence fails.
     */
    fun register(username: String, password: String, confirmPassword: String): Result<User>

    /**
     * Create an administrator user if one does not already exist.
     *
     * This is typically used at application bootstrap to ensure an admin account is present.
     *
     * @param username Desired admin username.
     * @param password Desired admin password.
     * @return Result containing the admin User on success, or a failure describing the problem.
     */
    fun createAdminIfNotExists(username: String, password: String): Result<User>
}


@DomainService
class UserFeatureImpl(
    private val userRepository: UserRepository,
    private val session: SessionManager,
    private val hasher: Hasher,
    private val tokenGenerator: TokenGenerator
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
            LOGGER.info("User ${userWithPassword.user.username} logged")
            val accessToken = tokenGenerator.generateToken(userWithPassword.user.id, userWithPassword.user.username, userWithPassword.roles)
            session.addSession(user.id, accessToken)
            return success(user.withToken(accessToken.tokenValue))
        }
        LOGGER.warning("Failed to log user $pseudonym")
        return failure(ResultState.USER_UNAUTHORIZED, "Le pseudonyme ou le mot de passe est incorrect")
    }

    override fun logout(token: String)
    : Result<Nothing> = session.authenticate(token) {
        session.removeSession(it, token)
        success()
    }

    override fun register(username: String, password: String, confirmPassword: String): Result<User> {
        if(password != confirmPassword) return failure(ResultState.PASSWORD_NOT_MATCH, "Les mots de passes ne correspondent pas")
        val hashedPassword = hasher.hash(password)
        val userResult = userRepository.register(username, hashedPassword) ?: return invalid("Une erreur est survenue")
        return success(userResult)
    }

    override fun createAdminIfNotExists(
        username: String,
        password: String
    ): Result<User> {
        val existingAdmin = userRepository.findByPseudonymWithEncodedPassword(username)
        val hashedPassword = hasher.hash(password)
        if(existingAdmin != null){
            LOGGER.info("Admin user already exists with username $username")
            return if (!hasher.verify(password, existingAdmin.password))
                failure(ResultState.PASSWORD_NOT_MATCH, "admin password does not match the existing one")
            else success(existingAdmin.user)
        }
        val adminUser = userRepository.register(username, hashedPassword, setOf(Role.USER, Role.ADMIN))
            ?: return invalid("Une erreur est survenue lors de la création de l'administrateur")
        LOGGER.info("Admin user created with username $username")
        return success(adminUser)
    }

}
