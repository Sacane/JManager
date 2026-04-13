package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class SessionManagerTest {

    val sessionManager: SessionManager = FakeFactory.sessionManager()
    val sessionState = FakeFactory.sessionState()
    val userState = FakeFactory.fakeUserRepository()

    @AfterEach
    fun after() {
        userState.clear()
    }

    @Nested
    inner class AddSessionTest {

        @Test
        fun `Add a session must return success`() {
            val id = UserId(UUID.randomUUID())
            userState.init(listOf(
                UserWithPassword(User(id, "test", email = "test"), "test")
            ))
            val accessToken = AccessToken(id, "test","${id.value}||${UUID.randomUUID()}||${Role.USER.name}||test")
            sessionManager.addSession(id, accessToken)
            sessionState.authenticate(SessionToken(accessToken.tokenValue)) {
                return@authenticate success("success")
            }.assertSuccess()
        }
    }

    @Test
    fun `Remove a session must return success`() {
        val id = UserId(UUID.randomUUID())
        userState.init(listOf(
            UserWithPassword(User(id, "test", email = "test"), "test")
        ))
        val accessToken = AccessToken(id, "test","${UUID.randomUUID()}||${UUID.randomUUID()}||${Role.USER.name}||test")
        sessionManager.addSession(id, accessToken)
        sessionManager.removeSession(id, SessionToken(accessToken.tokenValue))
        sessionManager.authenticate(SessionToken(accessToken.tokenValue)) {
            return@authenticate success("success")
        }.assertFailure()
    }

    @Nested
    inner class AuthenticateTest {

        @Test
        fun `Authenticate a session must return success`() {
            val id = UserId(UUID.randomUUID())
            userState.init(listOf(
                UserWithPassword(User(id, "test", email = "test"), "test")
            ))
            val accessToken = AccessToken(id, "test","${id.value}||${UUID.randomUUID()}||${Role.USER.name}||test")
            sessionManager.addSession(id, accessToken)
            sessionManager.authenticate(SessionToken(accessToken.tokenValue)) {
                return@authenticate success("success")
            }.assertSuccess()
        }
    }

    @Nested
    inner class RefreshTokenManagementTest {

        @Test
        fun `Save and authenticate refresh token must return success`() {
            val id = UserId(UUID.randomUUID())
            val refreshToken = UUID.randomUUID()

            sessionManager.saveRefreshToken(id, refreshToken, LocalDateTime.now().plusDays(1))

            sessionManager.authenticateRefreshToken(refreshToken) {
                return@authenticateRefreshToken success("success")
            }.assertSuccess()
        }

        @Test
        fun `Blacklisted refresh token must fail authentication`() {
            val id = UserId(UUID.randomUUID())
            val refreshToken = UUID.randomUUID()
            val expiresAt = LocalDateTime.now().plusDays(1)

            sessionManager.saveRefreshToken(id, refreshToken, expiresAt)
            sessionManager.blacklistRefreshToken(refreshToken, expiresAt)

            sessionManager.authenticateRefreshToken(refreshToken) {
                return@authenticateRefreshToken success("success")
            }.assertFailure(ResultState.UNAUTHORIZED)
        }
    }
}