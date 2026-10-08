package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserToken
import fr.sacane.jmanager.domain.port.output.Hasher
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.usecase.SessionOpener
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
    private val hasher: Hasher,
    private val sessionOpener: SessionOpener,
) : LoginUseCase {

    companion object {
        private val log = LoggerFactory.getLogger(LoginService::class.java)
    }

    // Checked when the address is unknown, so that path costs a hash check too. Computed on first use:
    // only the first unknown sign-in after start-up takes longer to answer.
    private val decoyHash: String by lazy { hasher.hash("decoy-password-that-matches-nothing") }

    /** An unknown address and a wrong password fail alike, in answer and in time. */
    override fun handle(command: LoginCommand): Result<UserToken> {
        val account = userRepository.findByEmailWithEncodedPassword(command.email)
        val passwordMatches = hasher.verify(command.userPassword, account?.password ?: decoyHash)
        if (account != null && passwordMatches) {
            return success(sessionOpener.openFor(account.user, account.roles))
        }
        log.warn(
            if (account == null) "Authentication failed: no account for this address"
            else "Authentication failed: invalid credentials for an existing account"
        )
        return failure(
            ResultState.USER_UNAUTHORIZED,
            DomainError(ResultState.USER_UNAUTHORIZED.code, "domain.user.login.invalid_credentials", "L'adresse e-mail ou le mot de passe est incorrect")
        )
    }
}
