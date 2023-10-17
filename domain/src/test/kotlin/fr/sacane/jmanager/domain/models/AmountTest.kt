package fr.sacane.jmanager.domain.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class AmountTest {

    @Test
    fun `An Amount can be build from an integer, double or bigDecimal`() {
        val ten = 10.toAmount()
        val tenDouble = 10.toDouble().toAmount()
        val tenDecimal = BigDecimal(10).toAmount()
        assertEquals(ten, tenDouble)
        assertEquals(tenDouble, tenDecimal)
    }

    @Test
    fun `A representation of an Amount should be its value and its currency`() {
        val tenEuros = 10.toAmount("€")
        assertEquals("10 €", tenEuros.toString())
    }

    @Test
    fun `plus and minus operator should update the amount's value`() {
        val amount = 10.toAmount()
        amount += 20.toAmount()
        amount -= 10.toAmount()
        assertEquals(20.toAmount(), amount)
    }

    @Test
    fun `An Amount can be build from its string representation`() {
        val tenEurosAsString = "10 €"
        val tenEurosDotFiveAsString = "10.05 €"
        val oneHundredDollarsAsString = "100 $"

        val amountTenEuros = Amount.fromString(tenEurosAsString)
        val tenEurosDotFiveAmount = Amount.fromString(tenEurosDotFiveAsString)
        val oneHundredDollarsAmount = Amount.fromString(oneHundredDollarsAsString)

        assertEquals("10 €", amountTenEuros.toString())
        assertEquals("10.05 €", tenEurosDotFiveAmount.toString())
        assertEquals("100 $", oneHundredDollarsAmount.toString())
    }

    @Test
    fun `An Amount should throw an exception when its scale is greater than 2`() {
        assertThrows<IllegalStateException> {
            105.056.toAmount()
        }
        assertThrows<IllegalStateException> {
            1.1000.toAmount()
        }
    }

    @Test
    fun `An invalid amount input should not be able to be build`() {
        assertThrows<InvalidMoneyFormatException> {
            Amount.fromString("10291DIOIDZJ JI1E")
        }
        assertThrows<InvalidMoneyFormatException> {
            Amount.fromString("1029 p€")
        }
    }
}