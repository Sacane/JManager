package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.BiState
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.UserSessionEntry
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.port.api.UserFeature
import fr.sacane.jmanager.domain.port.spi.DefaultHasher
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UserFeatureTest: FeatureTest() {

    companion object {
        private val userFeature: UserFeature = FakeFactory.sessionFeature
        private val sessionFakeState = FakeFactory.sessionState()
        private val userState = FakeFactory.fakeUserRepository()
    }

    @AfterEach
    fun afterEach() {
       userState.clear()
    }
    @Nested
    inner class LoginFeatureTest {

        @Test
        fun `Login a user must return success`() {
            val user = User(UserId(1), "John", "john.doe@gmail.com")
            userState.init(listOf(
                UserWithPassword(user, DefaultHasher.hash("test"))
            ))
            userFeature.login("John", "test")
                .assertSuccess()
        }
        @Test
        fun `Login a user with incorrect password must lead to unauthorized user result`() {
            val user = User(UserId(1), "John", "")
            userState.init(listOf(
                UserWithPassword(user, DefaultHasher.hash("test"))
            ))
            userFeature.login("John", "wrong")
                .assertFailure(ResultState.USER_UNAUTHORIZED)
        }
    }
    @Nested
    inner class LogoutFeatureTest {
        @Test
        fun `Logout a user must return success`() {
            val user = User(UserId(1), "John", "")
            userState.init(
                listOf(
                    UserWithPassword(user, DefaultHasher.hash("test"))
                )
            )
            sessionFakeState.addSession(user.id, session)
            userFeature.logout(user.id, session.tokenValue)
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
            userFeature.register("John", "test", "wrong")
                .assertFailure(ResultState.PASSWORD_NOT_MATCH)
        }
    }

    @Nested
    inner class TryRefreshFeatureTest {
        @Test
        fun `Try refresh a user must return success`() {
            val user = User(UserId(1), "John", "")
            userState.init(
                listOf(
                    UserWithPassword(user, DefaultHasher.hash("test"))
                )
            )
            sessionFakeState.addSession(user.id, session)
            userFeature.tryRefresh(user.id, session.refreshToken!!)
                .assertSuccess()
        }
    }
}