package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainImplementation
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.SessionRepository
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import java.util.logging.Logger

@Port(Side.API)
sealed interface Administrator {
    fun login(pseudonym: String, userPassword: Password): Response<UserToken>
    fun logout(userId: UserId, userToken: Token): Response<Nothing>
    fun register(user: User): Response<User>
}

@DomainImplementation
class LoginManager(
    private val loginRepository: SessionRepository,
    private val userTransaction: UserTransaction
): Administrator{

    companion object{
        private val LOGGER = Logger.getLogger(SessionRepository::class.java.name)
    }
    override fun login(pseudonym: String, userPassword: Password): Response<UserToken> {
        LOGGER.info("Trying to login user : $pseudonym")
        val user = userTransaction.findByPseudonym(pseudonym) ?: return Response.notFound("L'utilisateur $pseudonym n'existe pas")
        if(userPassword.matchWith(user.password)) {
            LOGGER.info("User $pseudonym logged")
            val token = loginRepository.generateToken(user) ?: return Response.invalid()
            return Response.ok(user.withToken(token))
        }
        LOGGER.warning("Failed to log user $pseudonym")
        return Response.forbidden("Le pseudonyme ou le mot de passe est incorrect")
    }

    override fun logout(userId: UserId, userToken: Token): Response<Nothing> {
        loginRepository.logout(userId, userToken) ?: return Response.invalid()
        return Response.ok()
    }

    override fun register(user: User): Response<User> {
        val userResponse = userTransaction.register(user) ?: return Response.invalid()
        return Response.ok(userResponse)
    }

}