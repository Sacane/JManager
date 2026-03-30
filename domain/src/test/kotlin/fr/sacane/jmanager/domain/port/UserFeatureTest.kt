package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.models.AccountMonthlyCycleUpdate
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.port.api.UserFeature
import fr.sacane.jmanager.domain.port.spi.DefaultHasher
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class UserFeatureTest: FeatureTest() {

    companion object {
        private val userFeature: UserFeature = FakeFactory.sessionFeature
        private val sessionFakeState = FakeFactory.sessionState()
        private val userState = FakeFactory.fakeUserRepository()
        private val tokenGenerator = FakeFactory.tokenGenerator
    }

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
            userFeature.login("John", "test")
                .assertSuccess()
        }
        @Test
        fun `Login a user with incorrect password must lead to unauthorized user result`() {
            val user = User(UserId(UUID.randomUUID()), "John", "")
            userState.init(listOf(
                UserWithPassword(user, DefaultHasher.hash("test"))
            ))
            val result = userFeature.login("John", "wrong")

            result.assertFailure(ResultState.USER_UNAUTHORIZED)
            assertEquals("domain.user.login.invalid_credentials", result.errorInfo?.key)
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
            userFeature.logout(token.tokenValue)
                .assertSuccess()
        }
    }

    @Nested
    inner class RegisterFeatureTest {
        @Test
        fun `Register a user must return success`() {
            userFeature.register("John", "test", "test")
                .assertSuccess()
        }
        @Test
        fun `Register a user with different password must return password not match`() {
            val result = userFeature.register("John", "test", "wrong")

            result.assertFailure(ResultState.PASSWORD_NOT_MATCH)
            assertEquals("domain.user.register.password_mismatch", result.errorInfo?.key)
        }
    }

    @Nested
    inner class CreateAdminFeatureTest {
        @Test
        fun `Create admin that doesn't exists must return success`() {
            userFeature.createAdminIfNotExists("admin", "test")
                .assertSuccess()
        }

        @Test
        fun `Create admin that exists must return success`() {
            userFeature.createAdminIfNotExists("admin", "test")
            userFeature.createAdminIfNotExists("admin", "test")
                .assertSuccess()
        }
        @Test
        fun `Create admin with different password must return password not match`() {
            userFeature.createAdminIfNotExists("admin", "test")
            val result = userFeature.createAdminIfNotExists("admin", "wrong")

            result.assertFailure(ResultState.PASSWORD_NOT_MATCH)
            assertEquals("domain.user.admin.password_mismatch", result.errorInfo?.key)
        }
    }

    @Nested
    inner class UserSettingsFeatureTest {
        @Test
        fun `Get settings for a connected user must return defaults`() {
            userFeature.register("John", "test", "test")
            val token = userFeature.login("John", "test").mapNotNullOrFailure()!!.token

            userFeature.getSettings(token)
                .assertTrue {
                    projectionWindowDays == 15 && accountCycles.isEmpty()
                }
        }

        @Test
        fun `Update settings must persist projection window`() {
            userFeature.register("John", "test", "test")
            val token = userFeature.login("John", "test").mapNotNullOrFailure()!!.token

            userFeature.updateSettings(
                token = token,
                projectionWindowDays = 30,
                accountCycles = emptyMap(),
            ).assertTrue {
                projectionWindowDays == 30
            }
        }

        @Test
        fun `Update settings with projection outside range must fail`() {
            userFeature.register("John", "test", "test")
            val token = userFeature.login("John", "test").mapNotNullOrFailure()!!.token

            val result = userFeature.updateSettings(
                token = token,
                projectionWindowDays = 6,
                accountCycles = emptyMap(),
            )

            result.assertFailure(ResultState.INVALID)
            assertEquals("domain.user.settings.invalid_projection_window", result.errorInfo?.key)
        }

        @Test
        fun `Update settings with non owned account must fail`() {
            userFeature.register("John", "test", "test")
            val token = userFeature.login("John", "test").mapNotNullOrFailure()!!.token

            val result = userFeature.updateSettings(
                token = token,
                projectionWindowDays = 20,
                accountCycles = mapOf(UUID.randomUUID() to AccountMonthlyCycleUpdate(10, null)),
            )

            result.assertFailure(ResultState.FORBIDDEN)
            assertEquals("domain.user.settings.account_forbidden", result.errorInfo?.key)
        }

        @Test
        fun `Update settings without cycle for each owned account must fail`() {
            launchWithConnectedUserInstance {
                val result = userFeature.updateSettings(
                    token = tokenValue,
                    projectionWindowDays = 20,
                    accountCycles = emptyMap(),
                )

                result.assertFailure(ResultState.INVALID)
                assertEquals("domain.user.settings.missing_account_cycles", result.errorInfo?.key)
            }
        }

        @Test
        fun `Update settings with invalid monthly period end day must fail`() {
            launchWithConnectedUserInstance {
                val result = userFeature.updateSettings(
                    token = tokenValue,
                    projectionWindowDays = 20,
                    accountCycles = mapOf(booklet.id!! to AccountMonthlyCycleUpdate(28, 40)),
                )

                result.assertFailure(ResultState.INVALID)
                assertEquals("domain.user.settings.invalid_monthly_period_end_day", result.errorInfo?.key)
            }
        }

        @Test
        fun `Update settings with cycle for each owned account must succeed`() {
            launchWithConnectedUserInstance {
                val result = userFeature.updateSettings(
                    token = tokenValue,
                    projectionWindowDays = 20,
                    accountCycles = mapOf(booklet.id!! to AccountMonthlyCycleUpdate(28, 27)),
                )

                result.assertTrue {
                    projectionWindowDays == 20
                            && accountCycles.size == 1
                            && accountCycles.first().monthlyPeriodStartDay == 28
                            && accountCycles.first().monthlyPeriodEndDay == 27
                }
            }
        }
    }
}