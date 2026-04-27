package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserToken
import fr.sacane.jmanager.domain.port.spi.Hasher
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.util.logging.Logger

data class LoginCommand(val pseudonym: String, val userPassword: String) : Command<UserToken>

@Port(Side.APPLICATION)
interface LoginUseCase : CommandHandler<LoginCommand, UserToken> {
    override val commandClass get() = LoginCommand::class
}

@DomainService
class LoginService(
    private val userRepository: UserRepository,
    private val session: SessionManager,
    private val hasher: Hasher,
    private val tokenGenerator: TokenGenerator
) : LoginUseCase {

    companion object {
        private val LOGGER = Logger.getLogger(LoginService::class.java.name)
    }

    override fun handle(command: LoginCommand): Result<UserToken> {
        val userWithPassword = userRepository.findByPseudonymWithEncodedPassword(command.pseudonym)
            ?: return failure(
                ResultState.NOT_FOUND,
                DomainError(ResultState.NOT_FOUND.code, "domain.user.login.user_not_found", "L'utilisateur ${command.pseudonym} n'existe pas")
            )
        LOGGER.info("LOGIN request for user ${userWithPassword.user.id}")
        val user = userWithPassword.user
        if (hasher.verify(command.userPassword, userWithPassword.password)) {
            LOGGER.info("User ${userWithPassword.user.username} logged")
            val accessToken = tokenGenerator.generateToken(userWithPassword.user.id, userWithPassword.user.username, userWithPassword.roles)
            session.addSession(user.id, accessToken)
            accessToken.refreshToken?.let {
                session.saveRefreshToken(user.id, it, accessToken.refreshTokenLifetime)
            }
            return success(user.withToken(accessToken.tokenValue, accessToken.refreshToken))
        }
        LOGGER.warning("Failed to log user ${command.pseudonym}")
        return failure(
            ResultState.USER_UNAUTHORIZED,
            DomainError(ResultState.USER_UNAUTHORIZED.code, "domain.user.login.invalid_credentials", "Le pseudonyme ou le mot de passe est incorrect")
        )
    }
}
