package fr.sacane.jmanager.domain.models

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CurrencyTest {

    @Test
    fun `Currency EUR should have correct symbol`() {
        assertEquals("€", Currency.EUR.symbol)
    }

    @Test
    fun `Currency USD should have correct symbol`() {
        assertEquals("$", Currency.USD.symbol)
    }

    @Test
    fun `Currency GPB should have correct symbol`() {
        assertEquals("£", Currency.GPB.symbol)
    }

    @Test
    fun `asList should return all currencies`() {
        val currencies = Currency.asList()

        assertEquals(3, currencies.size)
        assertTrue(currencies.contains(Currency.EUR))
        assertTrue(currencies.contains(Currency.USD))
        assertTrue(currencies.contains(Currency.GPB))
    }

    @Test
    fun `fromSymbolString should return correct currency for EUR`() {
        val currency = Currency.fromSymbolString("€")
        assertEquals(Currency.EUR, currency)
    }

    @Test
    fun `fromSymbolString should return correct currency for USD`() {
        val currency = Currency.fromSymbolString("$")
        assertEquals(Currency.USD, currency)
    }

    @Test
    fun `fromSymbolString should return correct currency for GPB`() {
        val currency = Currency.fromSymbolString("£")
        assertEquals(Currency.GPB, currency)
    }

    @Test
    fun `fromSymbolString should throw exception for invalid symbol`() {
        val exception = assertThrows<InvalidCurrencyException> {
            Currency.fromSymbolString("¥")
        }
        assertTrue(exception.message!!.contains("¥"))
        assertTrue(exception.message!!.contains("does not exists"))
    }

    @Test
    fun `asCurrency extension should convert symbol to currency`() {
        assertEquals(Currency.EUR, "€".asCurrency())
        assertEquals(Currency.USD, "$".asCurrency())
        assertEquals(Currency.GPB, "£".asCurrency())
    }

    @Test
    fun `asCurrency extension should throw exception for invalid symbol`() {
        assertThrows<InvalidCurrencyException> {
            "invalid".asCurrency()
        }
    }

    @Test
    fun `InvalidCurrencyException should be a RuntimeException`() {
        val exception = InvalidCurrencyException("test")
        assertEquals("test", exception.message)
    }
}

