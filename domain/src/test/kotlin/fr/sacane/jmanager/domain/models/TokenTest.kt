package fr.sacane.jmanager.domain.models


import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

class TokenTest {

    @Test
    fun `token should not be able to check its expiration when last refresh is not specified`(){
        val token = Token(UUID.randomUUID(), null, UUID.randomUUID())
        assertThrows<IllegalArgumentException> {
            token.isExpired()
        }
    }

    @Test
    fun `Ticket check identity should be effective for same token`(){
        val token = Token(UUID.randomUUID(), null, UUID.randomUUID())
        val user = User(UserId(0), "test", "test@gmail.com", mutableListOf(), Password("test"), mutableListOf())
        val ticket = UserToken(user, token)
        val verifiedToken = token.copy()
        assertFalse{
            ticket.checkForIdentity(verifiedToken) == null
        }
    }

    @Test
    fun `Token knows when it's expired or not`(){
        val token = Token(UUID.randomUUID(), LocalDateTime.now().plusSeconds(5), UUID.randomUUID())
        assertFalse(token.isExpired())
        Thread.sleep(5000)
        assertTrue(token.isExpired())
    }
}