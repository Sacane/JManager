package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserToken
import fr.sacane.jmanager.domain.port.output.Hasher
import fr.sacane.jmanager.domain.port.output.SessionManager
import fr.sacane.jmanager.domain.port.output.TokenGenerator
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import org.slf4j.LoggerFactory

data class LoginCommand(val email: String, val userPassword: String) : Command<UserToken>

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
        private val log = LoggerFactory.getLogger(LoginService::class.java)
    }

    override fun handle(command: LoginCommand): Result<UserToken> {
        val userWithPassword = userRepository.findByEmailWithEncodedPassword(command.email)
            ?: return failure(
                ResultState.NOT_FOUND,
                DomainError(ResultState.NOT_FOUND.code, "domain.user.login.user_not_found", "Aucun compte associé à l'adresse ${command.email}")
            )
        val user = userWithPassword.user
        if (hasher.verify(command.userPassword, userWithPassword.password)) {
            val accessToken = tokenGenerator.generateToken(userWithPassword.user.id, userWithPassword.user.username, userWithPassword.roles)
            session.addSession(user.id, accessToken)
            accessToken.refreshToken?.let {
                session.saveRefreshToken(user.id, it, accessToken.refreshTokenLifetime)
            }
            return success(user.withToken(accessToken.tokenValue, accessToken.refreshToken))
        }
        log.warn("Authentication failed: invalid credentials for an existing account")
        return failure(
            ResultState.USER_UNAUTHORIZED,
            DomainError(ResultState.USER_UNAUTHORIZED.code, "domain.user.login.invalid_credentials", "L'adresse e-mail ou le mot de passe est incorrect")
        )
    }
}
