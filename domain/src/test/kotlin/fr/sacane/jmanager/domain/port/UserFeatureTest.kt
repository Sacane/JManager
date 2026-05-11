package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.models.BookletMonthlyCycleUpdate
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
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
import fr.sacane.jmanager.domain.port.input.user.RegisterUserCommand
import fr.sacane.jmanager.domain.port.input.user.RegisterUserUseCase
import fr.sacane.jmanager.domain.port.input.user.UpdateUserSettingsCommand
import fr.sacane.jmanager.domain.port.input.user.UpdateUserSettingsUseCase
import fr.sacane.jmanager.domain.port.output.DefaultHasher
import fr.sacane.jmanager.domain.utils.success
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class UserFeatureTest: FeatureTest() {

    private val loginUseCase: LoginUseCase = factory.loginService
    private val logoutUseCase: LogoutUseCase = factory.logoutService
    private val refreshSessionUseCase: RefreshSessionUseCase = factory.refreshSessionService
    private val registerUserUseCase: RegisterUserUseCase = factory.registerUserService
    private val createAdminIfNotExistsUseCase: CreateAdminIfNotExistsUseCase = factory.createAdminIfNotExistsService
    private val getUserSettingsUseCase: GetUserSettingsUseCase = factory.getUserSettingsService
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase = factory.updateUserSettingsService
    private val sessionFakeState = factory.sessionState()
    private val userState = factory.fakeUserRepository()
    private val tokenGenerator = factory.tokenGenerator

    @AfterEach
    fun afterEach() {
       userState.clear()
    }
    @Nested
    inner class LoginFeatureTest {

        @Test
        fun `Login a user must return success`() {
            val user = User(UserId(UUID.randomUUID()), "John", "john.doe@gmail.com")
            userState.init(listOf(
                UserWithPassword(user, DefaultHasher.hash("test"))
            ))
            loginUseCase.handle(LoginCommand("John", "test"))
                .assertSuccess()
        }
        @Test
        fun `Login a user with incorrect password must lead to unauthorized user result`() {
            val user = User(UserId(UUID.randomUUID()), "John", "")
            userState.init(listOf(
                UserWithPassword(user, DefaultHasher.hash("test"))
            ))
            val result = loginUseCase.handle(LoginCommand("John", "wrong"))

            result.assertFailure(ResultState.USER_UNAUTHORIZED)
            assertEquals("domain.user.login.invalid_credentials", result.errorInfo?.key)
        }

        @Test
        fun `Login a user must persist refresh token in session manager`() {
            val user = User(UserId(UUID.randomUUID()), "John", "john.doe@gmail.com")
            userState.init(listOf(UserWithPassword(user, DefaultHasher.hash("test"))))

            val loginResult = loginUseCase.handle(LoginCommand("John", "test"))
            loginResult.assertSuccess()

            val accessToken = loginResult.mapNotNullOrFailure()!!.token
            val activeSession = sessionFakeState.findSessionByToken(SessionToken(accessToken))
            val refreshToken = activeSession?.refreshToken
            assertNotNull(refreshToken)

            sessionFakeState.authenticateRefreshToken(refreshToken!!) {
                return@authenticateRefreshToken success("ok")
            }.assertSuccess()
        }
    }
    @Nested
    inner class LogoutFeatureTest {
        @Test
        fun `Logout a user must return success`() {
            val user = User(UserId(UUID.randomUUID()), "John", "")
            userState.init(
                listOf(
                    UserWithPassword(user, DefaultHasher.hash("test"))
                )
            )
            val token = tokenGenerator.generateToken(user.id, user.username)
            sessionFakeState.addSession(user.id, token)
            logoutUseCase.handle(LogoutCommand(user.id, SessionToken(token.tokenValue)))
                .assertSuccess()
        }

        @Test
        fun `Logout must blacklist current refresh token`() {
            val user = User(UserId(UUID.randomUUID()), "John", "")
            userState.init(listOf(UserWithPassword(user, DefaultHasher.hash("test"))))

            val loginResult = loginUseCase.handle(LoginCommand("John", "test"))
            val accessToken = loginResult.mapNotNullOrFailure()!!.token
            val activeSession = sessionFakeState.findSessionByToken(SessionToken(accessToken))
            val refreshToken = activeSession?.refreshToken
            assertNotNull(refreshToken)

            logoutUseCase.handle(LogoutCommand(user.id, SessionToken(accessToken))).assertSuccess()

            sessionFakeState.authenticateRefreshToken(refreshToken!!) {
                return@authenticateRefreshToken success("ok")
            }.assertFailure(ResultState.UNAUTHORIZED)
        }
    }

    @Nested
    inner class RefreshFeatureTest {
        @Test
        fun `Refresh must rotate refresh token and blacklist previous one`() {
            val user = User(UserId(UUID.randomUUID()), "John", "john.doe@gmail.com")
            userState.init(listOf(UserWithPassword(user, DefaultHasher.hash("test"))))

            val loginResult = loginUseCase.handle(LoginCommand("John", "test"))
            val initialAccessToken = loginResult.mapNotNullOrFailure()!!.token
            val initialSession = sessionFakeState.findSessionByToken(SessionToken(initialAccessToken))
            val initialRefreshToken = initialSession?.refreshToken
            assertNotNull(initialRefreshToken)

            val refreshResult = refreshSessionUseCase.handle(RefreshSessionCommand(initialRefreshToken!!))
            refreshResult.assertSuccess()
            val refreshedAccessToken = refreshResult.mapNotNullOrFailure()!!.token

            val refreshedSession = sessionFakeState.findSessionByToken(SessionToken(refreshedAccessToken))
            val refreshedRefreshToken = refreshedSession?.refreshToken
            assertNotNull(refreshedRefreshToken)

            sessionFakeState.authenticateRefreshToken(initialRefreshToken) {
                return@authenticateRefreshToken success("ok")
            }.assertFailure(ResultState.UNAUTHORIZED)

            sessionFakeState.authenticateRefreshToken(refreshedRefreshToken!!) {
                return@authenticateRefreshToken success("ok")
            }.assertSuccess()
        }
    }

    @Nested
    inner class RegisterFeatureTest {
        @Test
        fun `Register a user must return success`() {
            registerUserUseCase.handle(RegisterUserCommand("John", "test", "test"))
                .assertSuccess()
        }
        @Test
        fun `Register a user with different password must return password not match`() {
            val result = registerUserUseCase.handle(RegisterUserCommand("John", "test", "wrong"))

            result.assertFailure(ResultState.PASSWORD_NOT_MATCH)
            assertEquals("domain.user.register.password_mismatch", result.errorInfo?.key)
        }
    }

    @Nested
    inner class CreateAdminFeatureTest {
        @Test
        fun `Create admin that doesn't exists must return success`() {
            createAdminIfNotExistsUseCase.handle(CreateAdminIfNotExistsCommand("admin", "test"))
                .assertSuccess()
        }

        @Test
        fun `Create admin that exists must return success`() {
            createAdminIfNotExistsUseCase.handle(CreateAdminIfNotExistsCommand("admin", "test"))
            createAdminIfNotExistsUseCase.handle(CreateAdminIfNotExistsCommand("admin", "test"))
                .assertSuccess()
        }
        @Test
        fun `Create admin with different password must return password not match`() {
            createAdminIfNotExistsUseCase.handle(CreateAdminIfNotExistsCommand("admin", "test"))
            val result = createAdminIfNotExistsUseCase.handle(CreateAdminIfNotExistsCommand("admin", "wrong"))

            result.assertFailure(ResultState.PASSWORD_NOT_MATCH)
            assertEquals("domain.user.admin.password_mismatch", result.errorInfo?.key)
        }
    }

    @Nested
    inner class UserSettingsFeatureTest {
        @Test
        fun `Get settings for a connected user must return defaults`() {
            registerUserUseCase.handle(RegisterUserCommand("John", "test", "test"))
            val userToken = loginUseCase.handle(LoginCommand("John", "test")).mapNotNullOrFailure()!!

            getUserSettingsUseCase.handle(GetUserSettingsQuery(userToken.user.id))
                .assertTrue {
                    projectionWindowDays == 15 && bookletCycles.isEmpty()
                }
        }

        @Test
        fun `Update settings must persist projection window`() {
            registerUserUseCase.handle(RegisterUserCommand("John", "test", "test"))
            val userToken = loginUseCase.handle(LoginCommand("John", "test")).mapNotNullOrFailure()!!

            updateUserSettingsUseCase.handle(
                UpdateUserSettingsCommand(
                    userId = userToken.user.id,
                    projectionWindowDays = 30,
                    bookletCycles = emptyMap(),
                )
            ).assertTrue {
                projectionWindowDays == 30
            }
        }

        @Test
        fun `Update settings with projection outside range must fail`() {
            registerUserUseCase.handle(RegisterUserCommand("John", "test", "test"))
            val userToken = loginUseCase.handle(LoginCommand("John", "test")).mapNotNullOrFailure()!!

            val result = updateUserSettingsUseCase.handle(
                UpdateUserSettingsCommand(
                    userId = userToken.user.id,
                    projectionWindowDays = 6,
                    bookletCycles = emptyMap(),
                )
            )

            result.assertFailure(ResultState.INVALID)
            assertEquals("domain.user.settings.invalid_projection_window", result.errorInfo?.key)
        }

        @Test
        fun `Update settings with non owned booklet must fail`() {
            registerUserUseCase.handle(RegisterUserCommand("John", "test", "test"))
            val userToken = loginUseCase.handle(LoginCommand("John", "test")).mapNotNullOrFailure()!!

            val result = updateUserSettingsUseCase.handle(
                UpdateUserSettingsCommand(
                    userId = userToken.user.id,
                    projectionWindowDays = 20,
                    bookletCycles = mapOf(UUID.randomUUID() to BookletMonthlyCycleUpdate(10, null)),
                )
            )

            result.assertFailure(ResultState.FORBIDDEN)
            assertEquals("domain.user.settings.booklet_forbidden", result.errorInfo?.key)
        }

        @Test
        fun `Update settings without cycle for each owned booklet must fail`() {
            launchWithUserId {
                val result = updateUserSettingsUseCase.handle(
                    UpdateUserSettingsCommand(
                        userId = userId,
                        projectionWindowDays = 20,
                        bookletCycles = emptyMap(),
                    )
                )

                result.assertFailure(ResultState.INVALID)
                assertEquals("domain.user.settings.missing_booklet_cycles", result.errorInfo?.key)
            }
        }

        @Test
        fun `Update settings with invalid monthly period end day must fail`() {
            launchWithUserId {
                val result = updateUserSettingsUseCase.handle(
                    UpdateUserSettingsCommand(
                        userId = userId,
                        projectionWindowDays = 20,
                        bookletCycles = mapOf(booklet.id!! to BookletMonthlyCycleUpdate(28, 40)),
                    )
                )

                result.assertFailure(ResultState.INVALID)
                assertEquals("domain.user.settings.invalid_monthly_period_end_day", result.errorInfo?.key)
            }
        }

        @Test
        fun `Update settings with cycle for each owned booklet must succeed`() {
            launchWithUserId {
                val result = updateUserSettingsUseCase.handle(
                    UpdateUserSettingsCommand(
                        userId = userId,
                        projectionWindowDays = 20,
                        bookletCycles = mapOf(booklet.id!! to BookletMonthlyCycleUpdate(28, 27)),
                    )
                )

                result.assertTrue {
                    projectionWindowDays == 20
                            && bookletCycles.size == 1
                            && bookletCycles.first().monthlyPeriodStartDay == 28
                            && bookletCycles.first().monthlyPeriodEndDay == 27
                }
            }
        }
    }
}
