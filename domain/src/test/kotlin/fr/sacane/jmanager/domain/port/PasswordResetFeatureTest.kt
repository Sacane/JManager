package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.act
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.models.PasswordResetToken
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.port.input.user.ConfirmPasswordResetCommand
import fr.sacane.jmanager.domain.port.input.user.LoginCommand
import fr.sacane.jmanager.domain.port.input.user.RefreshSessionCommand
import fr.sacane.jmanager.domain.port.input.user.RequestPasswordResetCommand
import fr.sacane.jmanager.domain.port.input.user.ValidatePasswordResetTokenQuery
import fr.sacane.jmanager.domain.port.output.DefaultHasher
import fr.sacane.jmanager.domain.then
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

private const val EMAIL = "johan@example.com"
private const val OLD_PASSWORD = "old-password-123"
private const val NEW_PASSWORD = "new-password-456"
private const val TOKEN_INVALID = "domain.user.password_reset.token_invalid"
private const val TOKEN_EXPIRED = "domain.user.password_reset.token_expired"

class PasswordResetFeatureTest {

    private val factory = FakeFactory()
    private val userState = factory.fakeUserRepository()
    private val tokenState = factory.passwordResetTokenState()
    private val notifications = factory.fakeNotificationPort
    private val now: LocalDateTime = LocalDateTime.now(factory.fixedClock)

    @AfterEach
    fun afterEach() = factory.clearAll()

    @Nested
    inner class RequestTest {

        @Test
        fun `shouldIssueATokenAndSendIt_whenAddressBelongsToAnEnabledUser`() {
            val user = givenUser()

            val result = act { requestReset(EMAIL) }

            then(result) { assertSuccess() }
            val stored = tokenState.all().single()
            assertEquals(user.id, stored.userId)
            assertEquals(now.plusMinutes(30), stored.expiresAt)
            assertEquals(EMAIL, notifications.sentPasswordResetEmails.single().email)
        }

        @Test
        fun `shouldPersistOnlyTheDigestOfTheTokenSentByEmail`() {
            givenUser()

            requestReset(EMAIL)

            val raw = notifications.sentPasswordResetEmails.single().token
            val stored = tokenState.all().single()
            assertNotEquals(raw, stored.tokenHash)
            assertEquals(PasswordResetToken.digestOf(raw), stored.tokenHash)
            assertEquals(64, stored.tokenHash.length)
        }

        @Test
        fun `shouldFindTheAccount_whenAddressDiffersOnlyByCase`() {
            val user = givenUser(email = "Johan@Example.com")

            requestReset("johan@example.com")

            assertEquals(user.id, tokenState.all().single().userId)
            assertEquals("Johan@Example.com", notifications.sentPasswordResetEmails.single().email)
        }

        @Test
        fun `shouldSucceedWithoutEffect_whenAddressIsUnknown`() {
            val result = act { requestReset("nobody@example.com") }

            then(result) { assertSuccess() }
            assertNothingIssued()
        }

        @Test
        fun `shouldSucceedWithoutEffect_whenAccountIsDisabled`() {
            givenUser(enabled = false)

            val result = act { requestReset(EMAIL) }

            then(result) { assertSuccess() }
            assertNothingIssued()
        }

        @Test
        fun `shouldSucceedWithoutEffect_whenAddressIsAmbiguous`() {
            givenUser(email = "Johan@Example.com")
            givenUser(email = "johan@example.com")

            val result = act { requestReset("JOHAN@EXAMPLE.COM") }

            then(result) { assertSuccess() }
            assertNothingIssued()
        }

        @Test
        fun `shouldReplaceThePreviousToken_whenANewResetIsRequested`() {
            givenUser()
            requestReset(EMAIL)
            val first = notifications.sentPasswordResetEmails.single().token

            requestReset(EMAIL)

            assertEquals(1, tokenState.all().size)
            then(confirm(first)) { assertFailure(ResultState.PASSWORD_RESET_TOKEN_INVALID) }
        }
    }

    @Nested
    inner class ConfirmTest {

        @Test
        fun `shouldSetTheNewPasswordAndConsumeTheToken_whenTokenIsValid`() {
            val user = givenUser(mustChangePassword = true)
            val token = issuedToken()

            val result = act { confirm(token) }

            then(result) { assertSuccess() }
            val stored = userState.findByIdWithEncodedPassword(user.id)!!
            assertTrue(DefaultHasher.verify(NEW_PASSWORD, stored.password))
            assertFalse(stored.user.mustChangePassword)
            assertTrue(tokenState.all().isEmpty())
        }

        @Test
        fun `shouldMarkTheAddressVerified_whenResetSucceeds`() {
            val user = givenUser(emailVerified = false)

            confirm(issuedToken())

            assertTrue(userState.findUserById(user.id)!!.emailVerified)
        }

        @Test
        fun `shouldRevokeEveryOlderCredential_whenResetSucceeds`() {
            val user = givenUser()
            val session = factory.loginService.handle(LoginCommand(EMAIL, OLD_PASSWORD)).mapNotNullOrFailure()!!

            confirm(issuedToken())

            assertEquals(now, userState.findUserById(user.id)!!.credentialsChangedAt)
            then(factory.refreshSessionService.handle(RefreshSessionCommand(session.refreshToken!!))) {
                assertFailure(ResultState.UNAUTHORIZED)
            }
        }

        @Test
        fun `shouldSendThePasswordChangedEmail_whenResetSucceeds`() {
            givenUser()

            confirm(issuedToken())

            val sent = notifications.sentPasswordChangedEmails.single()
            assertEquals(EMAIL, sent.email)
            assertEquals(now, sent.changedAt)
        }

        @Test
        fun `shouldRefuseAndKeepThePassword_whenTokenIsExpired`() {
            val user = givenUser()
            val raw = "expired-token"
            tokenState.save(PasswordResetToken(PasswordResetToken.digestOf(raw), user.id, now.minusSeconds(1)))

            val result = act { confirm(raw) }

            then(result) {
                assertFailure(ResultState.PASSWORD_RESET_TOKEN_EXPIRED)
                assertEquals(TOKEN_EXPIRED, errorInfo?.key)
            }
            assertTrue(DefaultHasher.verify(OLD_PASSWORD, userState.findByIdWithEncodedPassword(user.id)!!.password))
        }

        @Test
        fun `shouldRefuseAndKeepThePassword_whenTokenIsUnknown`() {
            val user = givenUser()

            val result = act { confirm("never-issued") }

            then(result) {
                assertFailure(ResultState.PASSWORD_RESET_TOKEN_INVALID)
                assertEquals(TOKEN_INVALID, errorInfo?.key)
            }
            assertTrue(DefaultHasher.verify(OLD_PASSWORD, userState.findByIdWithEncodedPassword(user.id)!!.password))
        }

        @Test
        fun `shouldRefuseAConsumedToken`() {
            givenUser()
            val token = issuedToken()
            confirm(token)

            then(confirm(token)) { assertFailure(ResultState.PASSWORD_RESET_TOKEN_INVALID) }
        }

        // A stale link must say so before asking anything else of the user.
        @Test
        fun `shouldReportTheExpiryFirst_whenPasswordsAlsoDiffer`() {
            val user = givenUser()
            val raw = "expired-token"
            tokenState.save(PasswordResetToken(PasswordResetToken.digestOf(raw), user.id, now.minusSeconds(1)))

            val result = act { confirm(raw, confirmPassword = "something-else") }

            then(result) { assertFailure(ResultState.PASSWORD_RESET_TOKEN_EXPIRED) }
        }

        @Test
        fun `shouldRefuseAndKeepTheTokenUsable_whenPasswordsDiffer`() {
            givenUser()
            val token = issuedToken()

            then(confirm(token, confirmPassword = "something-else")) { assertFailure(ResultState.PASSWORD_NOT_MATCH) }
            then(confirm(token)) { assertSuccess() }
        }

        @Test
        fun `shouldRefuseAndKeepTheTokenUsable_whenPasswordBreaksThePolicy`() {
            givenUser()
            val token = issuedToken()

            then(confirm(token, newPassword = "short", confirmPassword = "short")) {
                assertFailure(ResultState.PASSWORD_POLICY_VIOLATION)
                assertEquals(listOf("too_short"), errorInfo?.reasons)
            }
            then(confirm(token)) { assertSuccess() }
        }
    }

    @Nested
    inner class ValidateTest {

        @Test
        fun `shouldReportAValidTokenAndKeepItUsable`() {
            givenUser()
            val token = issuedToken()

            then(factory.validatePasswordResetTokenService.handle(ValidatePasswordResetTokenQuery(token))) { assertSuccess() }
            then(confirm(token)) { assertSuccess() }
        }

        @Test
        fun `shouldReportAStaleTokenWithItsReason`() {
            val user = givenUser()
            val raw = "expired-token"
            tokenState.save(PasswordResetToken(PasswordResetToken.digestOf(raw), user.id, now.minusSeconds(1)))

            then(factory.validatePasswordResetTokenService.handle(ValidatePasswordResetTokenQuery(raw))) {
                assertFailure(ResultState.PASSWORD_RESET_TOKEN_EXPIRED)
            }
            then(factory.validatePasswordResetTokenService.handle(ValidatePasswordResetTokenQuery("never-issued"))) {
                assertFailure(ResultState.PASSWORD_RESET_TOKEN_INVALID)
            }
        }
    }

    private fun givenUser(
        email: String = EMAIL,
        enabled: Boolean = true,
        emailVerified: Boolean = true,
        mustChangePassword: Boolean = false,
    ): User {
        val user = UserFixture.aUser(
            email = email,
            emailVerified = emailVerified,
            mustChangePassword = mustChangePassword,
            isEnabled = enabled,
        )
        userState.initWith(UserFixture.aUserWithPassword(user, DefaultHasher.hash(OLD_PASSWORD)))
        return user
    }

    private fun issuedToken(): String {
        requestReset(EMAIL)
        return notifications.sentPasswordResetEmails.last().token
    }

    private fun requestReset(email: String) = factory.requestPasswordResetService.handle(RequestPasswordResetCommand(email))

    private fun confirm(token: String, newPassword: String = NEW_PASSWORD, confirmPassword: String = newPassword) =
        factory.confirmPasswordResetService.handle(ConfirmPasswordResetCommand(token, newPassword, confirmPassword))

    private fun assertNothingIssued() {
        assertTrue(tokenState.all().isEmpty())
        assertTrue(notifications.sentPasswordResetEmails.isEmpty())
    }
}
