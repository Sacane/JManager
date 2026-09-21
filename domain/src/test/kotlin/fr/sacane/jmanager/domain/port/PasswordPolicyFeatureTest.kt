package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.act
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.port.input.admin.AdminCreateUserCommand
import fr.sacane.jmanager.domain.port.input.user.ChangePasswordCommand
import fr.sacane.jmanager.domain.port.input.user.CreateAdminIfNotExistsCommand
import fr.sacane.jmanager.domain.port.input.user.ForceChangePasswordCommand
import fr.sacane.jmanager.domain.port.input.user.LoginCommand
import fr.sacane.jmanager.domain.port.input.user.RegisterUserCommand
import fr.sacane.jmanager.domain.port.output.DefaultHasher
import fr.sacane.jmanager.domain.then
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

private const val COMPLIANT = "correct-horse-battery"
private const val TOO_SHORT = "short-pass"
private const val POLICY_KEY = "domain.user.password.policy_violation"

/** Every way of choosing a password applies the same policy; checking one at sign-in never does. */
class PasswordPolicyFeatureTest {

    private val factory = FakeFactory()
    private val userState = factory.fakeUserRepository()

    @AfterEach
    fun afterEach() = factory.clearAll()

    @Test
    fun `shouldRefuseRegistration_whenPasswordIsTooShort`() {
        val result = act { factory.registerUserService.handle(registration(password = TOO_SHORT)) }

        then(result) {
            assertFailure(ResultState.PASSWORD_POLICY_VIOLATION)
            assertEquals(POLICY_KEY, errorInfo?.key)
            assertEquals(listOf("too_short"), errorInfo?.reasons)
        }
        assertNull(userState.findByEmailWithEncodedPassword("john@example.com"))
        assertTrue(factory.fakeNotificationPort.sentCombinedEmails.isEmpty())
    }

    @Test
    fun `shouldReportEveryUnmetRule_whenRegistrationPasswordIsTheAddress`() {
        val result = act {
            factory.registerUserService.handle(registration(password = "a@b.fr", email = "a@b.fr"))
        }

        then(result) { assertEquals(listOf("too_short", "equals_email"), errorInfo?.reasons) }
    }

    @Test
    fun `shouldReportMismatchFirst_whenRegistrationPasswordsDifferAndAreTooShort`() {
        val result = act {
            factory.registerUserService.handle(registration(password = TOO_SHORT, confirmPassword = "other"))
        }

        then(result) { assertFailure(ResultState.PASSWORD_NOT_MATCH) }
    }

    @Test
    fun `shouldRegister_whenPasswordIsCompliant`() {
        val result = act { factory.registerUserService.handle(registration(password = COMPLIANT)) }

        then(result) { assertSuccess() }
    }

    @Test
    fun `shouldRefuseChange_whenNewPasswordIsTooShort`() {
        val user = UserFixture.aUser(email = "john@example.com")
        userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("current-password")))

        val result = act {
            factory.changePasswordService.handle(ChangePasswordCommand(user.id, "current-password", TOO_SHORT, TOO_SHORT))
        }

        then(result) {
            assertFailure(ResultState.PASSWORD_POLICY_VIOLATION)
            assertEquals(listOf("too_short"), errorInfo?.reasons)
        }
        assertTrue(DefaultHasher.verify("current-password", userState.findByIdWithEncodedPassword(user.id)!!.password))
    }

    // A wrong current password must not reveal whether the new one would have been accepted.
    @Test
    fun `shouldReportWrongCurrentPasswordFirst_whenNewPasswordIsAlsoTooShort`() {
        val user = UserFixture.aUser(email = "john@example.com")
        userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("current-password")))

        val result = act {
            factory.changePasswordService.handle(ChangePasswordCommand(user.id, "wrong-password", TOO_SHORT, TOO_SHORT))
        }

        then(result) { assertFailure(ResultState.USER_UNAUTHORIZED) }
    }

    @Test
    fun `shouldRefuseForcedChange_whenNewPasswordIsTheAccountAddress`() {
        val user = UserFixture.aUser(email = "john.doe.longer@example.com", mustChangePassword = true)
        userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("temporary-password")))

        val result = act {
            factory.forceChangePasswordService.handle(
                ForceChangePasswordCommand(user.id, "JOHN.DOE.LONGER@example.com", "JOHN.DOE.LONGER@example.com"),
            )
        }

        then(result) {
            assertFailure(ResultState.PASSWORD_POLICY_VIOLATION)
            assertEquals(listOf("equals_email"), errorInfo?.reasons)
        }
    }

    @Test
    fun `shouldRefuseAdminCreation_whenTemporaryPasswordIsTooShort`() {
        val result = act {
            factory.adminCreateUserUseCase.handle(AdminCreateUserCommand("jane", TOO_SHORT, "jane@example.com"))
        }

        then(result) {
            assertFailure(ResultState.PASSWORD_POLICY_VIOLATION)
            assertEquals(listOf("too_short"), errorInfo?.reasons)
        }
        assertNull(userState.findByEmailWithEncodedPassword("jane@example.com"))
    }

    // Its password comes from the environment at start-up: refusing it would stop the application booting.
    @Test
    fun `shouldCreateBootstrapAdmin_whenItsPasswordIsShorterThanThePolicy`() {
        val result = act { factory.createAdminIfNotExistsService.handle(CreateAdminIfNotExistsCommand("admin", "admin")) }

        then(result) { assertSuccess() }
    }

    @Test
    fun `shouldSignIn_whenExistingPasswordPredatesThePolicy`() {
        val user = UserFixture.aUser(email = "john@example.com")
        userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("old")))

        val result = act { factory.loginService.handle(LoginCommand(email = "john@example.com", userPassword = "old")) }

        then(result) { assertSuccess() }
    }

    private fun registration(
        password: String,
        confirmPassword: String = password,
        email: String = "john@example.com",
    ): RegisterUserCommand {
        val consentAt = LocalDateTime.of(2026, 1, 1, 10, 0)
        return RegisterUserCommand(
            username = "john",
            password = password,
            confirmPassword = confirmPassword,
            email = email,
            tosAcceptedAt = consentAt,
            tosVersion = "1.0",
            privacyAcceptedAt = consentAt,
        )
    }
}
