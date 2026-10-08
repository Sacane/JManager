package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.fixture.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PendingAccountStepTest {

    @Test
    fun `shouldHaveNoPendingStep_whenAccountConsentedAndChoseItsPassword`() {
        val user = UserFixture.aUserWithConsent()

        assertNull(user.pendingStep())
    }

    @Test
    fun `shouldRequireConsent_whenAccountNeverConsented`() {
        val user = UserFixture.aUser()

        assertEquals(PendingAccountStep.CONSENT, user.pendingStep())
    }

    @Test
    fun `shouldRequirePasswordChange_whenAccountConsentedButHoldsATemporaryPassword`() {
        val user = UserFixture.aUserWithConsent().apply { mustChangePassword = true }

        assertEquals(PendingAccountStep.PASSWORD_CHANGE, user.pendingStep())
    }

    // Same order as the screens: the terms come before the new password.
    @Test
    fun `shouldRequireConsentFirst_whenBothStepsArePending`() {
        val user = UserFixture.aUser(mustChangePassword = true)

        assertEquals(PendingAccountStep.CONSENT, user.pendingStep())
    }

    @Test
    fun `shouldExposeAStableErrorKeyPerStep`() {
        assertEquals("domain.user.pending_step.consent_required", PendingAccountStep.CONSENT.errorKey)
        assertEquals("domain.user.pending_step.password_change_required", PendingAccountStep.PASSWORD_CHANGE.errorKey)
    }
}
