package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.act
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.port.input.admin.AdminCreateUserCommand
import fr.sacane.jmanager.domain.port.input.user.ChangePasswordCommand
import fr.sacane.jmanager.domain.port.input.user.ChangePasswordUseCase
import fr.sacane.jmanager.domain.port.input.user.ForceChangePasswordCommand
import fr.sacane.jmanager.domain.port.input.user.ForceChangePasswordUseCase
import fr.sacane.jmanager.domain.port.output.DefaultHasher
import fr.sacane.jmanager.domain.then
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PasswordChangeFeatureTest {

    private val factory = FakeFactory()
    private val userState = factory.fakeUserRepository()
    private val adminCreateUserUseCase = factory.adminCreateUserUseCase
    private val changePasswordUseCase: ChangePasswordUseCase = factory.changePasswordService
    private val forceChangePasswordUseCase: ForceChangePasswordUseCase = factory.forceChangePasswordService

    @AfterEach
    fun afterEach() {
        factory.clearAll()
    }

    @Nested
    inner class VoluntaryChangePasswordTest {

        @Test
        fun `Changing password with correct current password must return success`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("currentPass")))

            val result = act {
                changePasswordUseCase.handle(ChangePasswordCommand(user.id, "currentPass", "newPass123", "newPass123"))
            }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Changing password with wrong current password must return USER_UNAUTHORIZED`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("currentPass")))

            val result = act {
                changePasswordUseCase.handle(ChangePasswordCommand(user.id, "wrongPass", "newPass123", "newPass123"))
            }

            then(result) {
                assertFailure(ResultState.USER_UNAUTHORIZED)
                assertEquals("domain.user.password.invalid_credentials", errorInfo?.key)
            }
        }

        @Test
        fun `Changing password with mismatched new passwords must return PASSWORD_NOT_MATCH`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("currentPass")))

            val result = act {
                changePasswordUseCase.handle(ChangePasswordCommand(user.id, "currentPass", "newPass123", "differentPass"))
            }

            then(result) {
                assertFailure(ResultState.PASSWORD_NOT_MATCH)
                assertEquals("domain.user.password.mismatch", errorInfo?.key)
            }
        }

        @Test
        fun `Changing password with same password as current must return PASSWORD_UNCHANGED`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("currentPass")))

            val result = act {
                changePasswordUseCase.handle(ChangePasswordCommand(user.id, "currentPass", "currentPass", "currentPass"))
            }

            then(result) {
                assertFailure(ResultState.PASSWORD_UNCHANGED)
                assertEquals("domain.user.password.unchanged", errorInfo?.key)
            }
        }

        @Test
        fun `Changing password must persist the new hashed password`() {
            val user = UserFixture.aUser()
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("currentPass")))

            act { changePasswordUseCase.handle(ChangePasswordCommand(user.id, "currentPass", "newPass123", "newPass123")) }

            val stored = userState.findByIdWithEncodedPassword(user.id)
            assertNotNull(stored)
            assertTrue(DefaultHasher.verify("newPass123", stored!!.password))
        }

        @Test
        fun `Changing password for unknown user must return USER_NOT_FOUND`() {
            val user = UserFixture.aUser()

            val result = act {
                changePasswordUseCase.handle(ChangePasswordCommand(user.id, "currentPass", "newPass123", "newPass123"))
            }

            then(result) { assertFailure(ResultState.USER_NOT_FOUND) }
        }
    }

    @Nested
    inner class MandatoryChangePasswordTest {

        @Test
        fun `Admin-created user has mustChangePassword set to true`() {
            val result = act {
                adminCreateUserUseCase.handle(AdminCreateUserCommand("newuser", "tempPass", "newuser@example.com"))
            }

            then(result) { assertTrue { mustChangePassword } }
        }

        @Test
        fun `Force change password with matching passwords must return success`() {
            val user = UserFixture.aUser(mustChangePassword = true)
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("tempPass")))

            val result = act {
                forceChangePasswordUseCase.handle(ForceChangePasswordCommand(user.id, "newPass123", "newPass123"))
            }

            then(result) { assertSuccess() }
        }

        @Test
        fun `Force change password must clear the mustChangePassword flag`() {
            val user = UserFixture.aUser(mustChangePassword = true)
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("tempPass")))

            act { forceChangePasswordUseCase.handle(ForceChangePasswordCommand(user.id, "newPass123", "newPass123")) }

            val updated = userState.findUserById(user.id)
            assertNotNull(updated)
            assertEquals(false, updated!!.mustChangePassword)
        }

        @Test
        fun `Force change password must persist the new hashed password`() {
            val user = UserFixture.aUser(mustChangePassword = true)
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("tempPass")))

            act { forceChangePasswordUseCase.handle(ForceChangePasswordCommand(user.id, "newPass123", "newPass123")) }

            val stored = userState.findByIdWithEncodedPassword(user.id)
            assertNotNull(stored)
            assertTrue(DefaultHasher.verify("newPass123", stored!!.password))
        }

        @Test
        fun `Force change password with mismatched passwords must return PASSWORD_NOT_MATCH`() {
            val user = UserFixture.aUser(mustChangePassword = true)
            userState.initWith(UserFixture.aUserWithPassword(user = user, password = DefaultHasher.hash("tempPass")))

            val result = act {
                forceChangePasswordUseCase.handle(ForceChangePasswordCommand(user.id, "newPass123", "differentPass"))
            }

            then(result) {
                assertFailure(ResultState.PASSWORD_NOT_MATCH)
                assertEquals("domain.user.password.mismatch", errorInfo?.key)
            }
        }

        @Test
        fun `Force change password for unknown user must return USER_NOT_FOUND`() {
            val user = UserFixture.aUser()

            val result = act {
                forceChangePasswordUseCase.handle(ForceChangePasswordCommand(user.id, "newPass123", "newPass123"))
            }

            then(result) { assertFailure(ResultState.USER_NOT_FOUND) }
        }
    }
}
