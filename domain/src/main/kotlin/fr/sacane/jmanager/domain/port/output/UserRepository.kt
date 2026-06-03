package fr.sacane.jmanager.domain.port.output

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.ConsentRecord
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.SubscriptionPlan
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.utils.Result
import java.time.LocalDateTime

@Port(Side.INFRASTRUCTURE)
/**
 * SPI contract for user persistence and retrieval operations.
 *
 * Implementations (adapters) provide concrete storage mechanisms (database, in-memory, LDAP, etc.).
 * The domain layer depends on this abstraction to fetch and persist user aggregates without
 * depending on any infrastructure details.
 */
interface UserRepository {
    /**
     * Find a user by domain identifier.
     *
     * @param userId domain UserId
     * @return User aggregate or null if not found
     */
    fun findUserById(userId: UserId): User?

    /**
     * Find a user by domain identifier and eagerly load their booklets.
     *
     * @param userId domain UserId
     * @return User aggregate with booklets or null if not found
     */
    fun findUserByIdWithBooklets(userId: UserId): User?

    /**
     * Find a user by their pseudonym/username.
     *
     * @param pseudonym username to search for
     * @return User aggregate or null if not found
     */
    fun findByPseudonym(pseudonym: String): User?

    /**
     * Find a user by pseudonym and include encoded password information required for authentication.
     *
     * @param pseudonym username to search for
     * @return UserWithPassword containing encoded credentials or null if not found
     */
    fun findByPseudonymWithEncodedPassword(pseudonym: String): UserWithPassword?

    /**
     * Find a user by email address and include encoded password information required for authentication.
     *
     * @param email email address to search for
     * @return UserWithPassword containing encoded credentials or null if not found
     */
    fun findByEmailWithEncodedPassword(email: String): UserWithPassword?

    /**
     * Create a user aggregate with encoded password information.
     *
     * @param user user aggregate with encoded password
     * @return persisted User or null on infrastructure failure
     */
    fun create(user: UserWithPassword): User?

    /**
     * Register a user with plain data (the implementation may hash the password) and default roles.
     *
     * @param username requested username
     * @param password encoded password (or hashed depending on convention)
     * @param roles Set of roles assigned to the user (defaults to Role.USER)
     * @return persisted User or null on failure
     */
    fun register(
        username: String,
        password: String,
        roles: Set<Role> = setOf(Role.USER),
        subscriptionPlan: SubscriptionPlan = SubscriptionPlan.BETA_TESTER,
        email: String? = null,
        tosAcceptedAt: LocalDateTime? = null,
        tosVersion: String? = null,
        privacyAcceptedAt: LocalDateTime? = null,
        emailVerified: Boolean = false,
        mustChangePassword: Boolean = false,
    ): User?

    /**
     * Mark a user's email address as verified.
     *
     * @param userId domain UserId of the user to update
     * @return success or USER_NOT_FOUND if the user does not exist
     */
    fun markEmailVerified(userId: UserId): Result<Unit>

    /**
     * Upsert (insert or update) a user aggregate.
     *
     * @param user user aggregate to save
     * @return persisted User or null on failure
     */
    fun upsert(user: User): User?

    /**
     * Update only the projection window preference for a user.
     *
     * @param userId domain UserId
     * @param projectionWindowDays projection window in days (7..60)
     * @return true when one row was updated, false otherwise
     */
    fun updateProjectionWindowDays(userId: UserId, projectionWindowDays: Int): Boolean

    /**
     * Retrieves all user aggregates stored in the repository.
     *
     * @return a list of all users in the repository, or an empty list if none exist.
     */
    fun findAll(): List<User>

    /**
     * Permanently delete a user and all their associated data (booklets, transactions, tags).
     * Implements the GDPR right to erasure (Art. 17).
     *
     * @param userId domain UserId of the account to delete
     * @return success or a failure result if the user was not found or deletion failed
     */
    fun deleteById(userId: UserId): Result<Unit>

    /**
     * Record explicit GDPR consent (Art. 6 / Art. 7) for a user.
     * Typically called at first login for admin-created accounts that bypassed the registration form.
     *
     * @param userId domain UserId of the consenting user
     * @param consent the consent record with timestamps and TOS version
     * @return success or a failure result if the user was not found
     */
    fun recordConsent(userId: UserId, consent: ConsentRecord): Result<Unit>

    /**
     * Find a user by domain identifier and include encoded password information.
     * Used by password-change flows that must verify the current credential.
     *
     * @param userId domain UserId
     * @return UserWithPassword or null if not found
     */
    fun findByIdWithEncodedPassword(userId: UserId): UserWithPassword?

    /**
     * Persist a new hashed password for the user.
     * When [clearMustChange] is true the mustChangePassword flag is also set to false,
     * marking the mandatory first-login change as fulfilled.
     *
     * @param userId domain UserId
     * @param hashedPassword BCrypt-hashed new password
     * @param clearMustChange whether to clear the mustChangePassword flag
     * @return success or USER_NOT_FOUND if the user does not exist
     */
    fun updatePassword(userId: UserId, hashedPassword: String, clearMustChange: Boolean): Result<Unit>
}