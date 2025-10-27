package fr.sacane.jmanager.domain

import fr.sacane.jmanager.domain.models.Amount
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class UtilsTest {

    @Test
    fun `asTokenUUID should remove Bearer prefix when present`() {
        val raw = "Bearer abc-def-123"
        val result = raw.asTokenUUID()
        assertEquals("abc-def-123", result)
    }

    @Test
    fun `asTokenUUID should return original string when no Bearer prefix`() {
        val raw = "no-prefix-token"
        val result = raw.asTokenUUID()
        assertEquals("no-prefix-token", result)
    }

    @Test
    fun `Env constants should have expected values`() {
        assertEquals(30L, Env.TOKEN_LIFETIME_IN_MINUTES)
        assertEquals(7L, Env.REFRESH_TOKEN_LIFETIME_IN_DAYS)
    }

    @Test
    fun `Long toAmount should build an Amount from long`() {
        val amount = 123L.toAmount()
        assertEquals(Amount(123L), amount)
    }

    @Test
    fun `String toUUID should parse a valid UUID string`() {
        val uuid = UUID.randomUUID()
        val parsed = uuid.toString().toUUID()
        assertEquals(uuid, parsed)
    }

    @Test
    fun `String toUUID should throw for invalid uuid string`() {
        assertThrows<IllegalArgumentException> {
            "not-a-uuid".toUUID()
        }
    }

    @Test
    fun `List of string toUUIDs should convert all elements`() {
        val u1 = UUID.randomUUID()
        val u2 = UUID.randomUUID()
        val list = listOf(u1.toString(), u2.toString())
        val uuids = list.toUUIDs()
        assertEquals(listOf(u1, u2), uuids)
    }
}