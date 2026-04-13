package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.weight
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.Result.Companion.unauthorized
import fr.sacane.jmanager.domain.utils.timeout
import java.time.LocalDateTime
import java.util.UUID
import java.util.logging.Logger

/**
 * SPI contract managing user sessions and token-based authentication used by the domain.
 *
 * Implementations are responsible for: storing active sessions, validating tokens,
 * checking required roles, handling session lifecycle (expiration, removal) and
 * exposing an `authenticate` helper to run domain use-cases inside an authenticated context.
 */
interface SessionManager{
    /**
     * Register a new active session for a user.
     *
     * @param userId Domain user identifier associated with the session.
     * @param session AccessToken value object representing the session.
     */
    fun addSession(userId: UserId, session: AccessToken)

    /**
     * Authenticate a token and execute the provided block with the authenticated UserId.
     *
     * Implementations should validate the token, check expiration and required roles.
     * If authentication fails, a domain Result indicating an unauthorized or timeout state
     * should be returned instead of throwing exceptions.
     *
     * @param token Raw token string provided by the caller.
     * @param requiredRoles List of roles required to execute the block (defaults to Role.USER).
     * @param block Function to execute when authentication succeeds; receives the authenticated UserId.
     * @return Result<T> produced by the block when authentication succeeds or a domain error Result otherwise.
     */
    fun <T> authenticate(
        token: SessionToken,
        requiredRoles: List<Role> = listOf(Role.USER),
        block: (UserId) -> Result<T>
    ): Result<T>

    /**
     * Removes an active session for the given user and token value.
     *
     * @param userId Domain user identifier.
     * @param token Raw token string to invalidate/remove.
     */
    fun removeSession(userId: UserId, token: SessionToken)

    /**
     * Find an active access-token session by raw token value.
     *
     * @param token Raw access token.
     * @return AccessToken session when found, null otherwise.
     */
    fun findSessionByToken(token: SessionToken): AccessToken?

    /**
     * Save a refresh token and associate it to a user.
     *
     * @param userId Domain user identifier.
     * @param refreshToken Refresh token identifier.
     * @param expiresAt Refresh token expiration instant.
     */
    fun saveRefreshToken(userId: UserId, refreshToken: UUID, expiresAt: LocalDateTime)

    /**
     * Authenticate a refresh token and execute the provided block with the associated user id.
     *
     * Implementations should reject blacklisted tokens and expired tokens.
     */
    fun <T> authenticateRefreshToken(refreshToken: UUID, block: (UserId) -> Result<T>): Result<T>

    /**
     * Retrieve refresh token expiration when present in store.
     *
     * @param refreshToken Refresh token identifier.
     * @return Expiration instant or null when token is unknown.
     */
    fun getRefreshTokenExpiry(refreshToken: UUID): LocalDateTime?

    /**
     * Blacklist (revoke) a refresh token until its expiry.
     *
     * @param refreshToken Refresh token identifier.
     * @param expiresAt Refresh token expiry, used for purge lifecycle.
     */
    fun blacklistRefreshToken(refreshToken: UUID, expiresAt: LocalDateTime)

    /**
     * Purge expired tokens/sessions from the session store.
     * Implementations may run this periodically.
     */
    fun purgeExpiredToken()
}

@DomainService
class InMemorySessionManager(private val tokenGenerator: TokenGenerator) : SessionManager {
    private val logger: Logger = Logger.getLogger(InMemorySessionManager::class.java.name)
    companion object {
        const val PURGE_DELAY = 1_800_000L // 30 minutes in milliseconds
    }

    private val lock = Any()
    private val userSession: MutableMap<UUID, MutableSet<AccessToken>> = mutableMapOf()
    private val refreshTokenStore: MutableMap<UUID, RefreshTokenSession> = mutableMapOf()
    private val blacklistedRefreshTokens: MutableMap<UUID, LocalDateTime> = mutableMapOf()

    private data class RefreshTokenSession(
        val userId: UserId,
        val expiresAt: LocalDateTime,
    )

    override fun addSession(userId: UserId, session: AccessToken): Unit = synchronized(lock){
        val sessions = userSession.computeIfAbsent(userId.value!!) { mutableSetOf() }
        sessions.add(session)
    }
    private fun getSession(userId: UserId, token: SessionToken): AccessToken? = synchronized(lock) {
        return try {
            userSession[userId.value]?.first { token.value == it.tokenValue }
        }catch (noSuchElementEx: NoSuchElementException){
            null
        }
    }
    override fun <T> authenticate(
        token: SessionToken,
        requiredRoles: List<Role>,
        block: (UserId) -> Result<T>
    ): Result<T> {
        val accessToken = synchronized(lock) {
            val decodedToken = tokenGenerator.readToken(token.value) ?: return unauthorized("Le token est invalide, une erreur est survenu à la lecture")
            val session = getSession(decodedToken.userId, SessionToken(decodedToken.tokenValue)) ?: return unauthorized("L'utilisateur n'est pas connecté à la session")

            val roles = session.roles
            if (roles.weight() < requiredRoles.weight()) return unauthorized("L'utilisateur n'a pas le rôle adéquat pour accéder à cette requête")
            if (session.isExpired()) return timeout("La session a expiré")
            if (session.tokenValue != token.value) return unauthorized("Le token est invalide")
            session.updateLifetime()
            session.updateTokenLifetime()
            decodedToken
        }
        return block(accessToken.userId)
    }

    override fun removeSession(userId: UserId, token: SessionToken): Unit = synchronized(lock){
        userSession[userId.value]?.removeIf{it.tokenValue == token.value}
    }

    override fun findSessionByToken(token: SessionToken): AccessToken? = synchronized(lock) {
        val decodedToken = tokenGenerator.readToken(token.value) ?: return null
        getSession(decodedToken.userId, SessionToken(decodedToken.tokenValue))
    }

    override fun saveRefreshToken(userId: UserId, refreshToken: UUID, expiresAt: LocalDateTime): Unit = synchronized(lock) {
        refreshTokenStore[refreshToken] = RefreshTokenSession(
            userId = userId,
            expiresAt = expiresAt,
        )
        blacklistedRefreshTokens.remove(refreshToken)
    }

    override fun <T> authenticateRefreshToken(refreshToken: UUID, block: (UserId) -> Result<T>): Result<T> {
        val userId = synchronized(lock) {
            val now = LocalDateTime.now()

            val blacklistedUntil = blacklistedRefreshTokens[refreshToken]
            if (blacklistedUntil != null) {
                return if (blacklistedUntil.isBefore(now)) {
                    blacklistedRefreshTokens.remove(refreshToken)
                    unauthorized("Le refresh token est invalide")
                } else {
                    unauthorized("Le refresh token est blacklisté")
                }
            }

            val refreshSession = refreshTokenStore[refreshToken]
                ?: return unauthorized("Le refresh token est invalide")

            if (refreshSession.expiresAt.isBefore(now)) {
                refreshTokenStore.remove(refreshToken)
                return timeout("Le refresh token a expiré")
            }

            refreshSession.userId
        }
        return block(userId)
    }

    override fun getRefreshTokenExpiry(refreshToken: UUID): LocalDateTime? = synchronized(lock) {
        refreshTokenStore[refreshToken]?.expiresAt
    }

    override fun blacklistRefreshToken(refreshToken: UUID, expiresAt: LocalDateTime): Unit = synchronized(lock) {
        refreshTokenStore.remove(refreshToken)
        blacklistedRefreshTokens[refreshToken] = expiresAt
    }

    override fun purgeExpiredToken() = synchronized(lock) {
        var counter = 0
        logger.info("Start purge expired tokens of => ${userSession.count()}")
        userSession.values.forEach {
            val result = it.removeIf { token ->
                token.isExpired()
            }
            if(result) counter++
        }
        userSession.entries.removeIf { (_, set) -> set.isEmpty() }

        val now = LocalDateTime.now()
        refreshTokenStore.entries.removeIf { (_, refreshSession) -> refreshSession.expiresAt.isBefore(now) }
        blacklistedRefreshTokens.entries.removeIf { (_, blacklistedUntil) -> blacklistedUntil.isBefore(now) }

        logger.info("Purge done, erased $counter tokens")
    }
}
