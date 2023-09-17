package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainImplementation
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.LoginManager
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import java.util.logging.Logger

sealed interface Administrator {
    fun login(pseudonym: String, userPassword: Password): Response<UserToken>
    fun logout(userId: UserId, userToken: Token): Response<Nothing>
    fun register(user: User): Response<User>
}

@DomainImplementation
class LoginManager(private val loginInventory: LoginManager, private val userTransaction: UserTransaction): Administrator{

    companion object{
        private val LOGGER = Logger.getLogger(LoginManager::class.java.name)
    }
    override fun login(pseudonym: String, userPassword: Password): Response<UserToken> {
        LOGGER.info("Trying to login user : $pseudonym")
        val user = userTransaction.findByPseudonym(pseudonym) ?: return Response.notFound("The user has not been find")
        val canLogin = userPassword.matchWith(user.password)
        if(canLogin) {
            LOGGER.info("User $pseudonym logged")
            val ticket = loginInventory.generateToken(user) ?: return Response.invalid()
            return Response.ok(UserToken(user, ticket))
        }
        LOGGER.info("Failed to log user $pseudonym")
        return Response.invalid()
    }

    override fun logout(userId: UserId, userToken: Token): Response<Nothing> {
        loginInventory.logout(userId, userToken) ?: return Response.invalid()
        return Response.ok()
    }

    override fun register(user: User): Response<User> {
        val userResponse = userTransaction.register(user) ?: return Response.invalid()
        return Response.ok(userResponse)
    }

}