package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.BookletMonthlyCycleUpdate
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserSettings
import fr.sacane.jmanager.domain.models.UserToken
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID

@Deprecated("Use individual use case interfaces from domain.port.input.user instead")
@Port(Side.APPLICATION)
/**
 * Application port: UserFeature
 *
 * High-level API for user authentication and user management use-cases exposed by the domain.
 * Implementations are responsible for handling authentication and returning domain Result<T>
 * objects signifying success or failure states.
 */
sealed interface UserFeature {
    /**
     * Authenticate a user with the given pseudonym and password.
     *
     * @param pseudonym The user's pseudonym or username used to identify the user.
     * @param userPassword The plain-text password to verify against the stored credentials.
     * @return Result containing a UserToken on success (including an access token), or an
     *         appropriate failure state (e.g. NOT_FOUND, USER_UNAUTHORIZED).
     */
    fun login(pseudonym: String, userPassword: String): Result<UserToken>

    /**
     * Invalidate a session identified by the provided token.
     *
     * @param token The session/access token to invalidate.
     * @return Result with no value on success, or an error if the token is invalid.
     */
    fun logout(token: SessionToken): Result<Nothing>

    /**
     * Refresh a session using a refresh token and issue a new access token.
     *
     * @param refreshToken Current valid refresh token.
     * @return Result containing a UserToken with a newly issued access token.
     */
    fun refresh(refreshToken: UUID): Result<UserToken>

    /**
     * Register a new user.
     *
     * @param username Desired username for the new user.
     * @param password Desired password for the new user (will be hashed by the domain).
     * @param confirmPassword Confirmation of the password; must match `password`.
     * @return Result containing the created User on success, or a failure state when
     *         validation or persistence fails.
     */
    fun register(username: String, password: String, confirmPassword: String): Result<User>

    /**
     * Create an administrator user if one does not already exist.
     *
     * This is typically used at application bootstrap to ensure an admin user is present.
     *
     * @param username Desired admin username.
     * @param password Desired admin password.
     * @return Result containing the admin User on success, or a failure describing the problem.
     */
    fun createAdminIfNotExists(username: String, password: String): Result<User>

    /**
     * Retrieve user settings used by dashboard calculations.
     *
     * @param token Authentication token identifying the requester.
     * @return Result containing the current settings on success.
     */
    fun getSettings(token: SessionToken): Result<UserSettings>

    /**
     * Update user settings for projection and booklet monthly cycles.
     *
     * @param token Authentication token identifying the requester.
     * @param projectionWindowDays global projection window in days (7..60)
     * @param bookletCycles monthly cycle configuration per booklet (start day required, end day optional)
     * @return Result containing updated settings on success.
     */
    fun updateSettings(
        token: SessionToken,
        projectionWindowDays: Int,
        bookletCycles: Map<UUID, BookletMonthlyCycleUpdate>
    ): Result<UserSettings>
}