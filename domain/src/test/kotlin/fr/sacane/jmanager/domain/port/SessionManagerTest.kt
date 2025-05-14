package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.utils.success
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
            val id = UserId(10)
            userState.init(listOf(
                UserWithPassword(User(id, "test", email = "test"), "test")
            ))
            val accessToken = AccessToken(id, "test","10||${UUID.randomUUID()}||${Role.USER.name}||test")
            sessionManager.addSession(id, accessToken)
            sessionState.authenticate(accessToken.tokenValue) {
                return@authenticate success("success")
            }.assertSuccess()
        }
    }
    @Test
    fun `Remove a session must return success`() {
        val id = UserId(10)
        userState.init(listOf(
            UserWithPassword(User(id, "test", email = "test"), "test")
        ))
        val accessToken = AccessToken(id, "test","10||${UUID.randomUUID()}||${Role.USER.name}||test")
        sessionManager.addSession(id, accessToken)
        sessionManager.removeSession(id, accessToken.tokenValue)
        sessionManager.authenticate(accessToken.tokenValue) {
            return@authenticate success("success")
        }.assertFailure()
    }

    @Nested
    inner class AuthenticateTest {

        @Test
        fun `Authenticate a session must return success`() {
            val id = UserId(10)
            userState.init(listOf(
                UserWithPassword(User(id, "test", email = "test"), "test")
            ))
            val accessToken = AccessToken(id, "test","10||${UUID.randomUUID()}||${Role.USER.name}||test")
            sessionManager.addSession(id, accessToken)
            sessionManager.authenticate(accessToken.tokenValue) {
                return@authenticate success("success")
            }.assertSuccess()
        }


    }
}