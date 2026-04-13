package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.Env
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.LocalDateTime
import java.util.UUID

class AccessTokenTest {

    private fun freshToken(): AccessToken = AccessToken(
        userId = UserId(UUID.randomUUID()),
        userName = "testUser",
        tokenValue = UUID.randomUUID().toString()
    )

    private fun expiredToken(): AccessToken = AccessToken(
        userId = UserId(UUID.randomUUID()),
        userName = "testUser",
        tokenValue = UUID.randomUUID().toString(),
        tokenExpirationDate = LocalDateTime.now().minusMinutes(1)
    )

    @Test
    fun `A fresh token must not be expired`() {
        val token = freshToken()
        assertFalse(token.isExpired())
    }

    @Test
    fun `A token with past expiration date must be expired`() {
        val token = expiredToken()
        assertTrue(token.isExpired())
    }

    @Test
    fun `updateLifetime must extend expiration to TOKEN_LIFETIME_IN_MINUTES from now`() {
        val token = freshToken()
        val before = LocalDateTime.now()

        token.updateLifetime()

        val expectedExpiry = before.plusMinutes(Env.TOKEN_LIFETIME_IN_MINUTES)
        assertAll(
            { assertFalse(token.isExpired()) },
            { assertTrue(token.tokenExpirationDate >= expectedExpiry) }
        )
    }

    @Test
    fun `updateLifetime on an expired token must make it valid again`() {
        val token = expiredToken()
        assertTrue(token.isExpired())

        token.updateLifetime()

        assertFalse(token.isExpired())
    }

    @Test
    fun `updateTokenLifetime must extend refresh token lifetime by REFRESH_TOKEN_LIFETIME_IN_DAYS`() {
        val token = freshToken()
        val previousRefreshLifetime = token.refreshTokenLifetime

        token.updateTokenLifetime()

        assertTrue(token.refreshTokenLifetime >= previousRefreshLifetime.plusDays(Env.REFRESH_TOKEN_LIFETIME_IN_DAYS))
    }
}
