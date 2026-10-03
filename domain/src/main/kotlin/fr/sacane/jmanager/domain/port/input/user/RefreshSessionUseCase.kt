package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserToken
import fr.sacane.jmanager.domain.port.output.SessionManager
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.usecase.SessionOpener
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID

data class RefreshSessionCommand(val refreshToken: UUID) : Command<UserToken>

@Port(Side.APPLICATION)
interface RefreshSessionUseCase : CommandHandler<RefreshSessionCommand, UserToken> {
    override val commandClass get() = RefreshSessionCommand::class
}

@DomainService
class RefreshSessionService(
    private val session: SessionManager,
    private val userRepository: UserRepository,
    private val sessionOpener: SessionOpener,
) : RefreshSessionUseCase {

    override fun handle(command: RefreshSessionCommand): Result<UserToken> =
        session.authenticateRefreshToken(command.refreshToken) { userId ->
            val refreshTokenExpiry = session.getRefreshTokenExpiry(command.refreshToken)
                ?: return@authenticateRefreshToken failure(
                    ResultState.UNAUTHORIZED,
                    DomainError(ResultState.UNAUTHORIZED.code, "domain.user.refresh.missing_refresh_token", "Refresh token introuvable")
                )

            val user = userRepository.findUserById(userId)
                ?: return@authenticateRefreshToken failure(
                    ResultState.USER_NOT_FOUND,
                    DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.refresh.user_not_found", "L'utilisateur n'existe pas")
                )

            session.blacklistRefreshToken(command.refreshToken, refreshTokenExpiry)
            success(sessionOpener.openFor(user, user.roles))
        }
}
