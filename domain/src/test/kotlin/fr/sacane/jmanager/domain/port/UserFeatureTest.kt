package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.act
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.models.BookletMonthlyCycleUpdate
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.input.user.CreateAdminIfNotExistsCommand
import fr.sacane.jmanager.domain.port.input.user.CreateAdminIfNotExistsUseCase
import fr.sacane.jmanager.domain.port.input.user.GetUserSettingsQuery
import fr.sacane.jmanager.domain.port.input.user.GetUserSettingsUseCase
import fr.sacane.jmanager.domain.port.input.user.LoginCommand
import fr.sacane.jmanager.domain.port.input.user.LoginUseCase
import fr.sacane.jmanager.domain.port.input.user.LogoutCommand
import fr.sacane.jmanager.domain.port.input.user.LogoutUseCase
import fr.sacane.jmanager.domain.port.input.user.RefreshSessionCommand
import fr.sacane.jmanager.domain.port.input.user.RefreshSessionUseCase
import fr.sacane.jmanager.domain.models.SubscriptionPlan
import fr.sacane.jmanager.domain.port.input.user.RegisterUserCommand
import fr.sacane.jmanager.domain.port.input.user.RegisterUserUseCase
import java.time.LocalDateTime
import fr.sacane.jmanager.domain.port.input.user.DeleteAccountCommand
import fr.sacane.jmanager.domain.port.input.user.DeleteAccountUseCase
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.input.user.HasUserConsentedQuery
import fr.sacane.jmanager.domain.port.input.user.HasUserConsentedUseCase
import fr.sacane.jmanager.domain.port.input.user.RecordConsentCommand
import fr.sacane.jmanager.domain.port.input.user.RecordConsentUseCase
import fr.sacane.jmanager.domain.port.input.user.UpdateUserSettingsCommand
import fr.sacane.jmanager.domain.port.input.user.UpdateUserSettingsUseCase
import fr.sacane.jmanager.domain.models.FeatureFlag
import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.domain.port.output.DefaultHasher
import fr.sacane.jmanager.domain.then
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

private fun registerCommand(
    username: String = "John",
    password: String = "test",
    confirmPassword: String = "test",
    email: String = "john@example.com",
    withConsent: Boolean = true,
) = RegisterUserCommand(
    username = username,
    password = password,
    confirmPassword = confirmPassword,
    email = email,
    tosAcceptedAt = if (withConsent) LocalDateTime.of(2026, 1, 1, 10, 0) else null,
    tosVersion = if (withConsent) "1.0" else null,
    privacyAcceptedAt = if (withConsent) LocalDateTime.of(2026, 1, 1, 10, 0) else null,
)

class UserFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val loginUseCase: LoginUseCase = factory.loginService
    private val logoutUseCase: LogoutUseCase = factory.logoutService
    private val refreshSessionUseCase: RefreshSessionUseCase = factory.refreshSessionService
    private val registerUserUseCase: RegisterUserUseCase = factory.registerUserService
    private val createAdminIfNotExistsUseCase: CreateAdminIfNotExistsUseCase = factory.createAdminIfNotExistsService
    private val getUserSettingsUseCase: GetUserSettingsUseCase = factory.getUserSettingsService
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase = factory.updateUserSettingsService
    private val deleteAccountUseCase: DeleteAccountUseCase = factory.deleteAccountService
    private val recordConsentUseCase: RecordConsentUseCase = factory.recordConsentService
    private val hasUserConsentedUseCase: HasUserConsentedUseCase = factory.hasUserConsentedService
    private val sessionFakeState = factory.sessionState()
    private val userState = factory.fakeUserRepository()
    private val tokenGenerator = factory.tokenGenerator
    private val sentEmails get() = factory.fakeNotificationPort.sentEmails

    @AfterEach
    fun afterEach() {
        factory.clearAll()
    }

    @Nested
    inner class LoginFeatureTest {

        @Test
        fun `Login a user must return success`() {
            val user = UserFixture.aUser(username = "John", email = "john.doe@gmail.com")
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("test")))

            val result = act { loginUseCase.handle(LoginCommand("John", "test")) }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Login a user with incorrect password must lead to unauthorized user result`() {
            val user = UserFixture.aUser(username = "John", email = "")
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("test")))

            val result = act { loginUseCase.handle(LoginCommand("John", "wrong")) }

            then(result) {
                assertFailure(ResultState.USER_UNAUTHORIZED)
                assertEquals("domain.user.login.invalid_credentials", errorInfo?.key)
            }
        }

        @Test
        fun `Login a user must persist refresh token in session manager`() {
            val user = UserFixture.aUser(username = "John", email = "john.doe@gmail.com")
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("test")))

            val loginResult = act { loginUseCase.handle(LoginCommand("John", "test")) }

            then(loginResult) {
                assertSuccess()
                val accessToken = mapNotNullOrFailure()!!.token
                val activeSession = sessionFakeState.findSessionByToken(SessionToken(accessToken))
                val refreshToken = activeSession?.refreshToken
                assertNotNull(refreshToken)

                sessionFakeState.authenticateRefreshToken(refreshToken!!) {
                    return@authenticateRefreshToken success("ok")
                }.assertSuccess()
            }
        }
    }

    @Nested
    inner class LogoutFeatureTest {
        @Test
        fun `Logout a user must return success`() {
            val u = UserFixture.aUser(username = "John", email = "")
            userState.initWith(UserFixture.aUserWithPassword(user = u, password = DefaultHasher.hash("test")))
            val t = tokenGenerator.generateToken(u.id, u.username)
            sessionFakeState.addSession(u.id, t)
            val (user, token) = u to t

            val result = act { logoutUseCase.handle(LogoutCommand(user.id, SessionToken(token.tokenValue))) }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Logout must blacklist current refresh token`() {
            val user = UserFixture.aUser(username = "John", email = "")
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("test")))

            val loginResult = loginUseCase.handle(LoginCommand("John", "test"))
            val accessToken = loginResult.mapNotNullOrFailure()!!.token
            val activeSession = sessionFakeState.findSessionByToken(SessionToken(accessToken))
            val refreshToken = activeSession?.refreshToken
            assertNotNull(refreshToken)

            act { logoutUseCase.handle(LogoutCommand(activeSession!!.userId, SessionToken(accessToken))).assertSuccess() }

            sessionFakeState.authenticateRefreshToken(refreshToken!!) {
                return@authenticateRefreshToken success("ok")
            }.assertFailure(ResultState.UNAUTHORIZED)
        }
    }

    @Nested
    inner class RefreshFeatureTest {
        @Test
        fun `Refresh must rotate refresh token and blacklist previous one`() {
            val user = UserFixture.aUser(username = "John", email = "john.doe@gmail.com")
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("test")))

            val loginResult = loginUseCase.handle(LoginCommand("John", "test"))
            val initialAccessToken = loginResult.mapNotNullOrFailure()!!.token
            val initialSession = sessionFakeState.findSessionByToken(SessionToken(initialAccessToken))
            val initialRefreshToken = initialSession?.refreshToken
            assertNotNull(initialRefreshToken)

            val refreshResult = act { refreshSessionUseCase.handle(RefreshSessionCommand(initialRefreshToken!!)) }

            then(refreshResult) {
                assertSuccess()
                val refreshedAccessToken = mapNotNullOrFailure()!!.token
                val refreshedSession = sessionFakeState.findSessionByToken(SessionToken(refreshedAccessToken))
                val refreshedRefreshToken = refreshedSession?.refreshToken
                assertNotNull(refreshedRefreshToken)

                sessionFakeState.authenticateRefreshToken(initialRefreshToken!!) {
                    return@authenticateRefreshToken success("ok")
                }.assertFailure(ResultState.UNAUTHORIZED)

                sessionFakeState.authenticateRefreshToken(refreshedRefreshToken!!) {
                    return@authenticateRefreshToken success("ok")
                }.assertSuccess()
            }
        }
    }

    @Nested
    inner class RegisterFeatureTest {
        @Test
        fun `Register a user must return success`() {
            val result = act { registerUserUseCase.handle(registerCommand()) }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Register a user with different password must return password not match`() {
            val result = act { registerUserUseCase.handle(registerCommand(confirmPassword = "wrong")) }

            then(result) {
                assertFailure(ResultState.PASSWORD_NOT_MATCH)
                assertEquals("domain.user.register.password_mismatch", errorInfo?.key)
            }
        }

        @Test
        fun `Registered user defaults to BETA_TESTER subscription plan`() {
            val result = act { registerUserUseCase.handle(registerCommand()) }

            then(result) {
                assertSuccess()
                assertEquals(SubscriptionPlan.BETA_TESTER, mapNotNullOrFailure()!!.subscriptionPlan)
            }
        }

        @Test
        fun `Welcome email is sent after successful registration`() {
            act { registerUserUseCase.handle(registerCommand()) }

            assertEquals(1, sentEmails.size)
            val sent = sentEmails.first()
            assertEquals("John", sent.username)
            assertEquals("john@example.com", sent.email)
            assertEquals(SubscriptionPlan.BETA_TESTER, sent.subscriptionPlan)
        }

        @Test
        fun `Welcome email is not sent when passwords do not match`() {
            act { registerUserUseCase.handle(registerCommand(confirmPassword = "wrong")) }

            assertEquals(0, sentEmails.size)
        }

        @Test
        fun `Welcome email is not sent when email is blank`() {
            act { registerUserUseCase.handle(registerCommand(email = "")) }

            assertEquals(0, sentEmails.size)
        }

        @Test
        fun `Registration fails when email is blank`() {
            val result = act { registerUserUseCase.handle(registerCommand(email = "")) }

            then(result) {
                assertFailure(ResultState.INVALID)
                assertEquals("domain.user.register.email_required", errorInfo?.key)
            }
        }

        @Test
        fun `Registration without TOS consent must return INVALID`() {
            val result = act { registerUserUseCase.handle(registerCommand(withConsent = false)) }

            then(result) {
                assertFailure(ResultState.INVALID)
                assertEquals("domain.user.register.consent_required", errorInfo?.key)
            }
        }

        @Test
        fun `Registration is rejected when USER_REGISTRATION flag is disabled`() {
            factory.inMemoryFeatureFlagRepository.upsert(FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = false))

            val result = act { registerUserUseCase.handle(registerCommand()) }

            then(result) {
                assertFailure(ResultState.FEATURE_DISABLED)
                assertEquals("domain.user.register.feature_disabled", errorInfo?.key)
            }
        }

        @Test
        fun `Registration is rejected when USER_REGISTRATION flag is absent from repository`() {
            factory.inMemoryFeatureFlagRepository.clear()

            val result = act { registerUserUseCase.handle(registerCommand()) }

            then(result) {
                assertFailure(ResultState.FEATURE_DISABLED)
                assertEquals("domain.user.register.feature_disabled", errorInfo?.key)
            }
        }

        @Test
        fun `No welcome email is sent when USER_REGISTRATION flag is disabled`() {
            factory.inMemoryFeatureFlagRepository.upsert(FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = false))

            act { registerUserUseCase.handle(registerCommand()) }

            assertEquals(0, sentEmails.size)
        }

        @Test
        fun `Registered user has consent record persisted`() {
            val consentTime = LocalDateTime.of(2026, 1, 1, 10, 0)
            val command = RegisterUserCommand(
                username = "John",
                password = "test",
                confirmPassword = "test",
                email = "john@example.com",
                tosAcceptedAt = consentTime,
                tosVersion = "1.0",
                privacyAcceptedAt = consentTime,
            )

            val result = act { registerUserUseCase.handle(command) }

            then(result) {
                assertSuccess()
                val user = mapNotNullOrFailure()!!
                assertNotNull(user.consent)
                assertEquals(consentTime, user.consent!!.tosAcceptedAt)
                assertEquals("1.0", user.consent!!.tosVersion)
                assertEquals(consentTime, user.consent!!.privacyAcceptedAt)
            }
        }
    }

    @Nested
    inner class CreateAdminFeatureTest {
        @Test
        fun `Create admin that doesn't exists must return success`() {
            val result = act { createAdminIfNotExistsUseCase.handle(CreateAdminIfNotExistsCommand("admin", "test")) }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Create admin that exists must return success`() {
            createAdminIfNotExistsUseCase.handle(CreateAdminIfNotExistsCommand("admin", "test"))

            val result = act { createAdminIfNotExistsUseCase.handle(CreateAdminIfNotExistsCommand("admin", "test")) }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Create admin with different password must return password not match`() {
            createAdminIfNotExistsUseCase.handle(CreateAdminIfNotExistsCommand("admin", "test"))

            val result = act { createAdminIfNotExistsUseCase.handle(CreateAdminIfNotExistsCommand("admin", "wrong")) }

            then(result) {
                assertFailure(ResultState.PASSWORD_NOT_MATCH)
                assertEquals("domain.user.admin.password_mismatch", errorInfo?.key)
            }
        }
    }

    @Nested
    inner class UserSettingsFeatureTest {
        @Test
        fun `Get settings for a connected user must return defaults`() {
            registerUserUseCase.handle(registerCommand())
            val userToken = loginUseCase.handle(LoginCommand("John", "test")).mapNotNullOrFailure()!!

            val result = act { getUserSettingsUseCase.handle(GetUserSettingsQuery(userToken.user.id)) }

            then(result) { assertTrue { projectionWindowDays == 15 && bookletCycles.isEmpty() } }
        }

        @Test
        fun `Update settings must persist projection window`() {
            registerUserUseCase.handle(registerCommand())
            val userToken = loginUseCase.handle(LoginCommand("John", "test")).mapNotNullOrFailure()!!

            val result = act {
                updateUserSettingsUseCase.handle(
                    UpdateUserSettingsCommand(userId = userToken.user.id, projectionWindowDays = 30, bookletCycles = emptyMap())
                )
            }

            then(result) { assertTrue { projectionWindowDays == 30 } }
        }

        @Test
        fun `Update settings with projection outside range must fail`() {
            registerUserUseCase.handle(registerCommand())
            val userToken = loginUseCase.handle(LoginCommand("John", "test")).mapNotNullOrFailure()!!

            val result = act {
                updateUserSettingsUseCase.handle(
                    UpdateUserSettingsCommand(userId = userToken.user.id, projectionWindowDays = 6, bookletCycles = emptyMap())
                )
            }

            then(result) {
                assertFailure(ResultState.INVALID)
                assertEquals("domain.user.settings.invalid_projection_window", errorInfo?.key)
            }
        }

        @Test
        fun `Update settings with non owned booklet must fail`() {
            registerUserUseCase.handle(registerCommand())
            val userToken = loginUseCase.handle(LoginCommand("John", "test")).mapNotNullOrFailure()!!

            val result = act {
                updateUserSettingsUseCase.handle(
                    UpdateUserSettingsCommand(
                        userId = userToken.user.id,
                        projectionWindowDays = 20,
                        bookletCycles = mapOf(UUID.randomUUID() to BookletMonthlyCycleUpdate(10, null)),
                    )
                )
            }

            then(result) {
                assertFailure(ResultState.FORBIDDEN)
                assertEquals("domain.user.settings.booklet_forbidden", errorInfo?.key)
            }
        }

        @Test
        fun `Update settings without cycle for each owned booklet must fail`() {
            val ctx = scenario.withUser(UserFixture.aUserWithPassword(password = "test")).withBooklet()

            val result = act {
                updateUserSettingsUseCase.handle(
                    UpdateUserSettingsCommand(userId = ctx.userId, projectionWindowDays = 20, bookletCycles = emptyMap())
                )
            }

            then(result) {
                assertFailure(ResultState.INVALID)
                assertEquals("domain.user.settings.missing_booklet_cycles", errorInfo?.key)
            }
        }

        @Test
        fun `Update settings with invalid monthly period end day must fail`() {
            val ctx = scenario.withUser(UserFixture.aUserWithPassword(password = "test")).withBooklet()

            val result = act {
                updateUserSettingsUseCase.handle(
                    UpdateUserSettingsCommand(
                        userId = ctx.userId,
                        projectionWindowDays = 20,
                        bookletCycles = mapOf(ctx.booklet.id!! to BookletMonthlyCycleUpdate(28, 40)),
                    )
                )
            }

            then(result) {
                assertFailure(ResultState.INVALID)
                assertEquals("domain.user.settings.invalid_monthly_period_end_day", errorInfo?.key)
            }
        }

        @Test
        fun `Update settings with cycle for each owned booklet must succeed`() {
            val ctx = scenario.withUser(UserFixture.aUserWithPassword(password = "test")).withBooklet()

            val result = act {
                updateUserSettingsUseCase.handle(
                    UpdateUserSettingsCommand(
                        userId = ctx.userId,
                        projectionWindowDays = 20,
                        bookletCycles = mapOf(ctx.booklet.id!! to BookletMonthlyCycleUpdate(28, 27)),
                    )
                )
            }

            then(result) {
                assertTrue {
                    projectionWindowDays == 20
                            && bookletCycles.size == 1
                            && bookletCycles.first().monthlyPeriodStartDay == 28
                            && bookletCycles.first().monthlyPeriodEndDay == 27
                }
            }
        }
    }

    @Nested
    inner class ConsentFeatureTest {

        private val consentTimestamp = LocalDateTime.of(2026, 1, 1, 10, 0)

        private fun recordCommand(
            userId: UserId,
            tosAccepted: Boolean = true,
            privacyAccepted: Boolean = true,
        ) = RecordConsentCommand(
            userId = userId,
            tosAccepted = tosAccepted,
            tosVersion = "1.0",
            privacyAccepted = privacyAccepted,
            consentTimestamp = consentTimestamp,
        )

        @Test
        fun `Recording consent with both accepted must return success`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user))

            val result = act { recordConsentUseCase.handle(recordCommand(user.id)) }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Recording consent must persist ConsentRecord on the user`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user))

            act { recordConsentUseCase.handle(recordCommand(user.id)) }

            val persisted = userState.findUserById(user.id)
            assertNotNull(persisted?.consent)
            assertEquals(consentTimestamp, persisted!!.consent!!.tosAcceptedAt)
            assertEquals("1.0", persisted.consent!!.tosVersion)
            assertEquals(consentTimestamp, persisted.consent!!.privacyAcceptedAt)
        }

        @Test
        fun `Recording consent for non-existent user must return USER_NOT_FOUND`() {
            val result = act { recordConsentUseCase.handle(recordCommand(UserFixture.aUser().id)) }

            then(result) { assertFailure(ResultState.USER_NOT_FOUND) }
        }

        @Test
        fun `Recording consent with TOS not accepted must return INVALID`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user))

            val result = act { recordConsentUseCase.handle(recordCommand(user.id, tosAccepted = false)) }

            then(result) {
                assertFailure(ResultState.INVALID)
                assertEquals("domain.user.consent.tos_required", errorInfo?.key)
            }
        }

        @Test
        fun `Recording consent with privacy policy not accepted must return INVALID`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user))

            val result = act { recordConsentUseCase.handle(recordCommand(user.id, privacyAccepted = false)) }

            then(result) {
                assertFailure(ResultState.INVALID)
                assertEquals("domain.user.consent.privacy_required", errorInfo?.key)
            }
        }

        @Test
        fun `HasUserConsentedQuery returns false for user without consent`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user))

            val result = act { hasUserConsentedUseCase.handle(HasUserConsentedQuery(user.id)) }

            then(result) { assertTrue { this == false } }
        }

        @Test
        fun `HasUserConsentedQuery returns true for user with existing consent`() {
            val user = UserFixture.aUserWithConsent()
            userState.initWith(UserFixture.aUserWithPassword(user = user))

            val result = act { hasUserConsentedUseCase.handle(HasUserConsentedQuery(user.id)) }

            then(result) { assertTrue { this == true } }
        }

        @Test
        fun `After recording consent HasUserConsentedQuery must return true`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user))
            recordConsentUseCase.handle(recordCommand(user.id))

            val result = act { hasUserConsentedUseCase.handle(HasUserConsentedQuery(user.id)) }

            then(result) { assertTrue { this == true } }
        }
    }

    @Nested
    inner class DeleteAccountFeatureTest {

        @Test
        fun `Deleting an existing account must return success`() {
            val user = UserFixture.aUser(username = "John", email = "john@example.com")
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("test")))

            val result = act { deleteAccountUseCase.handle(DeleteAccountCommand(user.id)) }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Deleting an existing account must remove the user from the repository`() {
            val user = UserFixture.aUser(username = "John", email = "john@example.com")
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("test")))

            act { deleteAccountUseCase.handle(DeleteAccountCommand(user.id)) }

            val found = userState.findUserById(user.id)
            assertEquals(null, found)
        }

        @Test
        fun `Deleting a non-existent account must return USER_NOT_FOUND`() {
            val result = act { deleteAccountUseCase.handle(DeleteAccountCommand(UserFixture.aUser().id)) }

            then(result) {
                assertFailure(ResultState.USER_NOT_FOUND)
                assertEquals("domain.user.delete.user_not_found", errorInfo?.key)
            }
        }
    }
}
