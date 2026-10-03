package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.act
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.input.user.ChangePasswordCommand
import fr.sacane.jmanager.domain.port.input.user.ForceChangePasswordCommand
import fr.sacane.jmanager.domain.port.input.user.LoginCommand
import fr.sacane.jmanager.domain.port.input.user.RefreshSessionCommand
import fr.sacane.jmanager.domain.port.output.DefaultHasher
import fr.sacane.jmanager.domain.then
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

private const val EMAIL = "john@example.com"
private const val CURRENT = "current-password"
private const val NEW = "new-password-123"

/** A new password ends every session opened with the old one, except the one making the change. */
class CredentialRevocationFeatureTest {

    private val factory = FakeFactory()
    private val userState = factory.fakeUserRepository()
    private val now: LocalDateTime = LocalDateTime.now(factory.fixedClock)

    @AfterEach
    fun afterEach() = factory.clearAll()

    @Test
    fun `shouldRecordTheChangeTime_whenPasswordIsChanged`() {
        val userId = givenUser()

        act { factory.changePasswordService.handle(ChangePasswordCommand(userId, CURRENT, NEW, NEW)) }

        assertEquals(now, userState.findUserById(userId)!!.credentialsChangedAt)
    }

    @Test
    fun `shouldRevokeEveryOtherRefreshToken_whenPasswordIsChanged`() {
        val userId = givenUser()
        val laptop = signIn()
        val phone = signIn()

        act { factory.changePasswordService.handle(ChangePasswordCommand(userId, CURRENT, NEW, NEW)) }

        then(refresh(laptop)) { assertFailure(ResultState.UNAUTHORIZED) }
        then(refresh(phone)) { assertFailure(ResultState.UNAUTHORIZED) }
    }

    @Test
    fun `shouldKeepTheChangingDeviceSignedIn_whenPasswordIsChanged`() {
        val userId = givenUser()

        val result = act { factory.changePasswordService.handle(ChangePasswordCommand(userId, CURRENT, NEW, NEW)) }

        then(result) { assertSuccess() }
        val session = result.mapNotNullOrFailure()!!
        assertEquals(userId, session.user.id)
        then(refresh(session.refreshToken!!)) { assertSuccess() }
    }

    @Test
    fun `shouldRevokeNothing_whenPasswordChangeIsRefused`() {
        val userId = givenUser()
        val laptop = signIn()

        act { factory.changePasswordService.handle(ChangePasswordCommand(userId, "wrong-password", NEW, NEW)) }

        assertNull(userState.findUserById(userId)!!.credentialsChangedAt)
        then(refresh(laptop)) { assertSuccess() }
    }

    @Test
    fun `shouldRevokeOlderSessionsAndKeepTheDevice_whenForcedChangeCompletes`() {
        val userId = givenUser(mustChangePassword = true)
        val laptop = signIn()

        val result = act { factory.forceChangePasswordService.handle(ForceChangePasswordCommand(userId, NEW, NEW)) }

        then(result) { assertSuccess() }
        assertEquals(now, userState.findUserById(userId)!!.credentialsChangedAt)
        then(refresh(laptop)) { assertFailure(ResultState.UNAUTHORIZED) }
        then(refresh(result.mapNotNullOrFailure()!!.refreshToken!!)) { assertSuccess() }
    }

    @Test
    fun `shouldRevokeNothing_whenForcedChangeIsRefused`() {
        val userId = givenUser(mustChangePassword = true)
        val laptop = signIn()

        act { factory.forceChangePasswordService.handle(ForceChangePasswordCommand(userId, NEW, "other-password-123")) }

        assertNull(userState.findUserById(userId)!!.credentialsChangedAt)
        then(refresh(laptop)) { assertSuccess() }
    }

    private fun givenUser(mustChangePassword: Boolean = false): UserId {
        val user = UserFixture.aUser(email = EMAIL, mustChangePassword = mustChangePassword)
        userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash(CURRENT)))
        return user.id
    }

    private fun signIn(): UUID {
        val session = factory.loginService.handle(LoginCommand(EMAIL, CURRENT)).mapNotNullOrFailure()
        assertNotNull(session)
        return session!!.refreshToken!!
    }

    private fun refresh(refreshToken: UUID) = factory.refreshSessionService.handle(RefreshSessionCommand(refreshToken))
}
