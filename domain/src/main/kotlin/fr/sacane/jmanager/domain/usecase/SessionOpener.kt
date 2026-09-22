package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserToken
import fr.sacane.jmanager.domain.port.output.SessionManager
import fr.sacane.jmanager.domain.port.output.TokenGenerator

/** Opens a session for a user: issues an access token and registers it with its refresh token. */
@DomainService
class SessionOpener(
    private val sessionManager: SessionManager,
    private val tokenGenerator: TokenGenerator,
) {
    fun openFor(user: User, roles: Set<Role>): UserToken {
        val accessToken = tokenGenerator.generateToken(user.id, user.username, roles)
        sessionManager.addSession(user.id, accessToken)
        accessToken.refreshToken?.let {
            sessionManager.saveRefreshToken(user.id, it, accessToken.refreshTokenLifetime)
        }
        return user.withToken(accessToken.tokenValue, accessToken.refreshToken)
    }
}
