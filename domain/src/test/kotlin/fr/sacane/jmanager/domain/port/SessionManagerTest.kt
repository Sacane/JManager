package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.act
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.output.SessionManager
import fr.sacane.jmanager.domain.then
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class SessionManagerTest {

    private val factory = FakeFactory()
    private val sessionManager: SessionManager = factory.sessionManager()
    private val userState = factory.fakeUserRepository()

    @AfterEach
    fun after() {
        factory.clearAll()
    }

    @Nested
    inner class AddSessionTest {

        @Test
        fun `Add a session must be findable by token`() {
            val id = UserId(UUID.randomUUID())
            userState.initWith(UserFixture.aUserWithPassword(user = UserFixture.aUser(id = id, username = "test", email = "test"), password = "test"))

            val accessToken = AccessToken(id, "test", "${id.value}||${UUID.randomUUID()}||${Role.USER.name}||test")
            sessionManager.addSession(id, accessToken)

            val found = act { sessionManager.findSessionByToken(SessionToken(accessToken.tokenValue)) }

            then(found) {
                assertNotNull(this)
                assertEquals(accessToken.tokenValue, this!!.tokenValue)
            }
        }
    }

    @Test
    fun `Remove a session must make it unfindable`() {
        val id = UserId(UUID.randomUUID())
        userState.initWith(UserFixture.aUserWithPassword(user = UserFixture.aUser(id = id, username = "test", email = "test"), password = "test"))

        val accessToken = AccessToken(id, "test", "${id.value}||${UUID.randomUUID()}||${Role.USER.name}||test")
        sessionManager.addSession(id, accessToken)

        act { sessionManager.removeSession(id, SessionToken(accessToken.tokenValue)) }

        val found = sessionManager.findSessionByToken(SessionToken(accessToken.tokenValue))
        assertNull(found)
    }

    @Nested
    inner class RefreshTokenManagementTest {

        @Test
        fun `Save and authenticate refresh token must return success`() {
            val id = UserId(UUID.randomUUID())
            val refreshToken = UUID.randomUUID()

            sessionManager.saveRefreshToken(id, refreshToken, LocalDateTime.now().plusDays(1))

            val result = act {
                sessionManager.authenticateRefreshToken(refreshToken) {
                    return@authenticateRefreshToken success("success")
                }
            }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Blacklisted refresh token must fail authentication`() {
            val id = UserId(UUID.randomUUID())
            val refreshToken = UUID.randomUUID()
            val expiresAt = LocalDateTime.now().plusDays(1)

            sessionManager.saveRefreshToken(id, refreshToken, expiresAt)
            sessionManager.blacklistRefreshToken(refreshToken, expiresAt)

            val result = act {
                sessionManager.authenticateRefreshToken(refreshToken) {
                    return@authenticateRefreshToken success("success")
                }
            }

            then(result) { assertFailure(ResultState.UNAUTHORIZED) }
        }
    }
}