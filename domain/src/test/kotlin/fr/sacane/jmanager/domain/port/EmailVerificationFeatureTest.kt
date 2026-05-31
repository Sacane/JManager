package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.act
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.port.input.user.RegisterUserCommand
import fr.sacane.jmanager.domain.port.input.user.ResendVerificationEmailCommand
import fr.sacane.jmanager.domain.port.input.user.VerifyEmailCommand
import fr.sacane.jmanager.domain.then
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

private fun registerCommand(
    username: String = "carol",
    email: String = "carol@example.com",
) = RegisterUserCommand(
    username = username,
    password = "pass",
    confirmPassword = "pass",
    email = email,
    tosAcceptedAt = LocalDateTime.of(2026, 1, 1, 10, 0),
    tosVersion = "1.0",
    privacyAcceptedAt = LocalDateTime.of(2026, 1, 1, 10, 0),
)

class EmailVerificationFeatureTest {

    private val factory = FakeFactory()
    private val userState = factory.fakeUserRepository()
    private val tokenState = factory.fakeEmailVerificationTokenRepository()
    private val verifyEmailUseCase = factory.verifyEmailService
    private val resendVerificationEmailUseCase = factory.resendVerificationEmailService
    private val registerUserUseCase = factory.registerUserService
    private val sentVerificationEmails get() = factory.fakeNotificationPort.sentVerificationEmails
    private val sentCombinedEmails get() = factory.fakeNotificationPort.sentCombinedEmails

    @AfterEach
    fun tearDown() {
        factory.clearAll()
    }

    @Nested
    inner class VerifyEmailTest {

        @Test
        fun `valid token marks user email as verified`() {
            val user = UserFixture.aUser(email = "alice@example.com")
            userState.initWith(UserFixture.aUserWithPassword(user = user))
            val rawToken = tokenState.issueFreshToken(user.id, factory.fixedClock)

            val result = act { verifyEmailUseCase.handle(VerifyEmailCommand(rawToken)) }

            then(result) { assertSuccess() }
            assertTrue(userState.findUserById(user.id)!!.emailVerified)
        }

        @Test
        fun `verifying a valid token deletes all tokens for the user`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user))
            val rawToken = tokenState.issueFreshToken(user.id, factory.fixedClock)

            act { verifyEmailUseCase.handle(VerifyEmailCommand(rawToken)) }

            assertFalse(tokenState.hasTokenForUser(user.id))
        }

        @Test
        fun `expired token is rejected and email remains unverified`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user))
            val rawToken = tokenState.issueExpiredToken(user.id)

            val result = act { verifyEmailUseCase.handle(VerifyEmailCommand(rawToken)) }

            then(result) {
                assertFailure(ResultState.EMAIL_VERIFICATION_TOKEN_EXPIRED)
                assertEquals("domain.user.email_verification.token_expired", errorInfo?.key)
            }
            assertFalse(userState.findUserById(user.id)!!.emailVerified)
        }

        @Test
        fun `unknown token is rejected`() {
            val result = act { verifyEmailUseCase.handle(VerifyEmailCommand("does-not-exist")) }

            then(result) {
                assertFailure(ResultState.NOT_FOUND)
                assertEquals("domain.user.email_verification.token_not_found", errorInfo?.key)
            }
        }
    }

    @Nested
    inner class ResendVerificationEmailTest {

        @Test
        fun `resend issues a fresh token and sends a new verification email`() {
            val user = UserFixture.aUser(email = "bob@example.com")
            userState.initWith(UserFixture.aUserWithPassword(user = user))

            val result = act { resendVerificationEmailUseCase.handle(ResendVerificationEmailCommand(user.id)) }

            then(result) { assertSuccess() }
            assertEquals(1, sentVerificationEmails.size)
            assertEquals("bob@example.com", sentVerificationEmails.first().email)
        }

        @Test
        fun `resend deletes the previous token before issuing a new one`() {
            val user = UserFixture.aUser(email = "bob@example.com")
            userState.initWith(UserFixture.aUserWithPassword(user = user))
            val oldRawToken = tokenState.issueFreshToken(user.id, factory.fixedClock)

            act { resendVerificationEmailUseCase.handle(ResendVerificationEmailCommand(user.id)) }

            assertEquals(1, tokenState.countForUser(user.id))
            assertFalse(tokenState.existsByToken(oldRawToken))
            val newToken = sentVerificationEmails.last().token
            assertTrue(tokenState.existsByToken(newToken))
        }

        @Test
        fun `resend is rejected for an already-verified user`() {
            val user = UserFixture.aUser(email = "verified@example.com", emailVerified = true)
            userState.initWith(UserFixture.aUserWithPassword(user = user))

            val result = act { resendVerificationEmailUseCase.handle(ResendVerificationEmailCommand(user.id)) }

            then(result) {
                assertFailure(ResultState.EMAIL_ALREADY_VERIFIED)
                assertEquals("domain.user.email_verification.already_verified", errorInfo?.key)
            }
            assertEquals(0, sentVerificationEmails.size)
        }

        @Test
        fun `resend for an unknown user returns USER_NOT_FOUND`() {
            val result = act {
                resendVerificationEmailUseCase.handle(ResendVerificationEmailCommand(UserFixture.aUser().id))
            }

            then(result) { assertFailure(ResultState.USER_NOT_FOUND) }
        }
    }

    @Nested
    inner class RegistrationWithVerificationTest {

        @Test
        fun `self-registered user is created with emailVerified false`() {
            val result = act { registerUserUseCase.handle(registerCommand()) }

            then(result) {
                assertSuccess()
                assertFalse(mapNotNullOrFailure()!!.emailVerified)
            }
        }

        @Test
        fun `self-registered user receives a single combined welcome and verification email`() {
            act { registerUserUseCase.handle(registerCommand(email = "dave@example.com")) }

            assertEquals(1, sentCombinedEmails.size)
            assertEquals("dave@example.com", sentCombinedEmails.first().email)
            assertEquals(0, sentVerificationEmails.size)
        }

        @Test
        fun `self-registration saves a verification token`() {
            val result = act { registerUserUseCase.handle(registerCommand()) }

            val userId = result.mapNotNullOrFailure()!!.id
            assertTrue(tokenState.hasTokenForUser(userId))
        }
    }
}
