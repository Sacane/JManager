package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success

@DomainService
class LogoutService(
    private val session: SessionManager
) : LogoutUseCase {

    override fun logout(token: SessionToken): Result<Nothing> = session.authenticate(token) {
        val activeSession = session.findSessionByToken(token)
        val refreshToken = activeSession?.refreshToken
        if (refreshToken != null) {
            session.blacklistRefreshToken(refreshToken, activeSession.refreshTokenLifetime)
        }
        session.removeSession(it, token)
        success()
    }
}
