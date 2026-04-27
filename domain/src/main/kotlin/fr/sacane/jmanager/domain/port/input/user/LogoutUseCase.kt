package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.output.SessionManager
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success

data class LogoutCommand(val token: SessionToken) : Command<Nothing>

@Port(Side.APPLICATION)
interface LogoutUseCase : CommandHandler<LogoutCommand, Nothing> {
    override val commandClass get() = LogoutCommand::class
}

@DomainService
class LogoutService(
    private val session: SessionManager
) : LogoutUseCase {

    override fun handle(command: LogoutCommand): Result<Nothing> = session.authenticate(command.token) {
        val activeSession = session.findSessionByToken(command.token)
        val refreshToken = activeSession?.refreshToken
        if (refreshToken != null) {
            session.blacklistRefreshToken(refreshToken, activeSession.refreshTokenLifetime)
        }
        session.removeSession(it, command.token)
        success()
    }
}
