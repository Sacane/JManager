package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.fixture.UserFixture
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

private val CHANGED_AT: LocalDateTime = LocalDateTime.of(2026, 9, 22, 10, 0, 0, 700_000_000)

class UserCredentialRevocationTest {

    @Test
    fun `shouldAcceptAnyToken_whenCredentialsNeverChanged`() {
        val user = UserFixture.aUser(credentialsChangedAt = null)

        assertTrue(user.acceptsTokenIssuedAt(CHANGED_AT.minusDays(1)))
        assertTrue(user.acceptsTokenIssuedAt(null))
    }

    @Test
    fun `shouldRefuseToken_whenIssuedBeforeTheChange`() {
        val user = UserFixture.aUser(credentialsChangedAt = CHANGED_AT)

        assertFalse(user.acceptsTokenIssuedAt(CHANGED_AT.minusSeconds(1)))
    }

    // Tokens issued before the issue time was recorded cannot prove they are recent.
    @Test
    fun `shouldRefuseToken_whenItHasNoIssueTimeAndCredentialsChanged`() {
        val user = UserFixture.aUser(credentialsChangedAt = CHANGED_AT)

        assertFalse(user.acceptsTokenIssuedAt(null))
    }

    // The issue time has second precision: the session re-issued by the change itself must pass.
    @Test
    fun `shouldAcceptToken_whenIssuedInTheSameSecondAsTheChange`() {
        val user = UserFixture.aUser(credentialsChangedAt = CHANGED_AT)

        assertTrue(user.acceptsTokenIssuedAt(CHANGED_AT.withNano(0)))
    }

    @Test
    fun `shouldAcceptToken_whenIssuedAfterTheChange`() {
        val user = UserFixture.aUser(credentialsChangedAt = CHANGED_AT)

        assertTrue(user.acceptsTokenIssuedAt(CHANGED_AT.plusMinutes(5)))
    }
}
