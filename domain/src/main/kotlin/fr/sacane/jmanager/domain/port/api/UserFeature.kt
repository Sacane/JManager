package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.AccountMonthlyCycleUpdate
import fr.sacane.jmanager.domain.models.UserSettings
import fr.sacane.jmanager.domain.models.UserToken
import fr.sacane.jmanager.domain.port.spi.Hasher
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID
import java.util.logging.Logger

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
     * @param pseudonym The user's pseudonym or username used to identify the account.
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
    fun logout(token: String): Result<Nothing>

    /**
     * Register a new user account.
     *
     * @param username Desired username for the new account.
     * @param password Desired password for the new account (will be hashed by the domain).
     * @param confirmPassword Confirmation of the password; must match `password`.
     * @return Result containing the created User on success, or a failure state when
     *         validation or persistence fails.
     */
    fun register(username: String, password: String, confirmPassword: String): Result<User>

    /**
     * Create an administrator user if one does not already exist.
     *
     * This is typically used at application bootstrap to ensure an admin account is present.
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
    fun getSettings(token: String): Result<UserSettings>

    /**
     * Update user settings for projection and account monthly cycles.
     *
     * @param token Authentication token identifying the requester.
     * @param projectionWindowDays global projection window in days (7..60)
        * @param accountCycles monthly cycle configuration per account (start day required, end day optional)
     * @return Result containing updated settings on success.
     */
    fun updateSettings(
        token: String,
        projectionWindowDays: Int,
        accountCycles: Map<UUID, AccountMonthlyCycleUpdate>
    ): Result<UserSettings>
}


@DomainService
class UserFeatureImpl(
    private val userRepository: UserRepository,
    private val bookletRepository: BookletRepository,
    private val session: SessionManager,
    private val hasher: Hasher,
    private val tokenGenerator: TokenGenerator
): UserFeature{

    companion object{
        private val LOGGER = Logger.getLogger(UserFeatureImpl::class.java.name)
    }

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun login(pseudonym: String, userPassword: String): Result<UserToken> {
        val userWithPassword = userRepository.findByPseudonymWithEncodedPassword(pseudonym)
            ?: return domainFailure(
                ResultState.NOT_FOUND,
                "L'utilisateur $pseudonym n'existe pas",
                "domain.user.login.user_not_found"
            )
        LOGGER.info("LOGIN request for user ${userWithPassword.user.id}")
        val user = userWithPassword.user
        if(hasher.verify(userPassword, userWithPassword.password)) {
            LOGGER.info("User ${userWithPassword.user.username} logged")
            val accessToken = tokenGenerator.generateToken(userWithPassword.user.id, userWithPassword.user.username, userWithPassword.roles)
            session.addSession(user.id, accessToken)
            return success(user.withToken(accessToken.tokenValue))
        }
        LOGGER.warning("Failed to log user $pseudonym")
        return domainFailure(
            ResultState.USER_UNAUTHORIZED,
            "Le pseudonyme ou le mot de passe est incorrect",
            "domain.user.login.invalid_credentials"
        )
    }

    override fun logout(token: String)
    : Result<Nothing> = session.authenticate(token) {
        session.removeSession(it, token)
        success()
    }

    override fun register(username: String, password: String, confirmPassword: String): Result<User> {
        if (password != confirmPassword) {
            return domainFailure(
                ResultState.PASSWORD_NOT_MATCH,
                "Les mots de passes ne correspondent pas",
                "domain.user.register.password_mismatch"
            )
        }
        val hashedPassword = hasher.hash(password)
        val userResult = userRepository.register(username, hashedPassword)
            ?: return domainFailure(
                ResultState.INVALID,
                "Une erreur est survenue",
                "domain.user.register.invalid"
            )
        return success(userResult)
    }

    override fun createAdminIfNotExists(
        username: String,
        password: String
    ): Result<User> {
        val existingAdmin = userRepository.findByPseudonymWithEncodedPassword(username)
        val hashedPassword = hasher.hash(password)
        if(existingAdmin != null){
            LOGGER.info("Admin user already exists with username $username")
            return if (!hasher.verify(password, existingAdmin.password))
                domainFailure(
                    ResultState.PASSWORD_NOT_MATCH,
                    "admin password does not match the existing one",
                    "domain.user.admin.password_mismatch"
                )
            else success(existingAdmin.user)
        }
        val adminUser = userRepository.register(username, hashedPassword, setOf(Role.USER, Role.ADMIN))
            ?: return domainFailure(
                ResultState.INVALID,
                "Une erreur est survenue lors de la création de l'administrateur",
                "domain.user.admin.creation_failed"
            )
        LOGGER.info("Admin user created with username $username")
        return success(adminUser)
    }

    override fun getSettings(token: String): Result<UserSettings> = session.authenticate(token) { userId ->
        val user = userRepository.findUserByIdWithAccounts(userId)
            ?: return@authenticate domainFailure(
                ResultState.USER_NOT_FOUND,
                "L'utilisateur n'existe pas",
                "domain.user.settings.user_not_found"
            )

        success(user.toSettings())
    }

    override fun updateSettings(
        token: String,
        projectionWindowDays: Int,
        accountCycles: Map<UUID, AccountMonthlyCycleUpdate>
    ): Result<UserSettings> = session.authenticate(token) { userId ->
        if (projectionWindowDays !in 7..60) {
            return@authenticate domainFailure(
                ResultState.INVALID,
                "La fenêtre de projection doit être comprise entre 7 et 60 jours",
                "domain.user.settings.invalid_projection_window"
            )
        }

        val invalidAccountCycle = accountCycles.entries.firstOrNull { (_, cycle) ->
            cycle.monthlyPeriodStartDay !in 1..31
        }
        if (invalidAccountCycle != null) {
            return@authenticate domainFailure(
                ResultState.INVALID,
                "Le jour de début de période doit être compris entre 1 et 31",
                "domain.user.settings.invalid_monthly_period_start_day"
            )
        }

        val invalidAccountCycleEndDay = accountCycles.entries.firstOrNull { (_, cycle) ->
            cycle.monthlyPeriodEndDay != null && cycle.monthlyPeriodEndDay !in 1..31
        }
        if (invalidAccountCycleEndDay != null) {
            return@authenticate domainFailure(
                ResultState.INVALID,
                "Le jour de fin de période doit être compris entre 1 et 31",
                "domain.user.settings.invalid_monthly_period_end_day"
            )
        }

        val user = userRepository.findUserByIdWithAccounts(userId)
            ?: return@authenticate domainFailure(
                ResultState.USER_NOT_FOUND,
                "L'utilisateur n'existe pas",
                "domain.user.settings.user_not_found"
            )

        val accountsById = user.booklets.mapNotNull { account ->
            account.id?.let { id -> id to account }
        }.toMap()

        val missingAccounts = accountsById.keys - accountCycles.keys
        if (missingAccounts.isNotEmpty()) {
            return@authenticate domainFailure(
                ResultState.INVALID,
                "Chaque compte doit avoir un cycle mensuel configuré",
                "domain.user.settings.missing_account_cycles"
            )
        }

        for ((accountId, accountCycle) in accountCycles) {
            val account = accountsById[accountId]
                ?: return@authenticate domainFailure(
                    ResultState.FORBIDDEN,
                    "Vous n'avez pas accès au compte $accountId",
                    "domain.user.settings.account_forbidden"
                )

            val updated = bookletRepository.updateMonthlyPeriodStartDay(
                accountId,
                accountCycle.monthlyPeriodStartDay,
                accountCycle.monthlyPeriodEndDay,
            )
            if (!updated) {
                return@authenticate domainFailure(
                    ResultState.INFRASTRUCTURE_ERROR,
                    "Impossible de mettre à jour le cycle du compte $accountId",
                    "domain.user.settings.account_update_failed"
                )
            }
            account.updateMonthlyPeriodConfiguration(
                accountCycle.monthlyPeriodStartDay,
                accountCycle.monthlyPeriodEndDay,
            )
        }

        val projectionUpdated = userRepository.updateProjectionWindowDays(userId, projectionWindowDays)
        if (!projectionUpdated) {
            return@authenticate domainFailure(
                ResultState.INFRASTRUCTURE_ERROR,
                "Impossible de mettre à jour la fenêtre de projection",
                "domain.user.settings.projection_update_failed"
            )
        }

        user.updateProjectionWindowDays(projectionWindowDays)
        success(user.toSettings())
    }

    private fun User.toSettings(): UserSettings = UserSettings(
        projectionWindowDays = projectionWindowDays,
        accountCycles = booklets.mapNotNull { booklet ->
            val accountId = booklet.id ?: return@mapNotNull null
            fr.sacane.jmanager.domain.models.AccountMonthlyCycleSetting(
                accountId = accountId,
                accountLabel = booklet.label,
                monthlyPeriodStartDay = booklet.monthlyPeriodStartDay,
                monthlyPeriodEndDay = booklet.monthlyPeriodEndDay,
            )
        }
    )

}
