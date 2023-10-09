package fr.sacane.jmanager.domain.models


import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

class TokenTest {
    @Test
    fun `Token knows when it's expired or not`(){
        val token = AccessToken(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID())
        assertFalse(token.isExpired())
        Thread.sleep(5000)
        assertTrue(token.isExpired())
    }
}