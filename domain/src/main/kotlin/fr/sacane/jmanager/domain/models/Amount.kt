package fr.sacane.jmanager.domain.models

import java.math.BigDecimal
import java.math.RoundingMode

data class Amount(var value: BigDecimal, val currency: Currency = Currency.EUR) {
    constructor(longAmount: Long) : this(BigDecimal(longAmount), currency =  Currency.EUR)

    init{
        if(value.scale() != 2){
            value = value.setScale(2, RoundingMode.HALF_UP)
        }
    }
    operator fun plusAssign(other: Amount) {
        value += other.value
    }
    operator fun minusAssign(other: Amount) {
        value -= other.value
    }
    operator fun plus(other: Amount): Amount {
        return Amount(other.value + value, currency)
    }
    operator fun minus(other: Amount): Amount{
        return Amount(value - other.value, currency)
    }
    operator fun timesAssign(other: Amount) {
        value *= other.value
    }
    operator fun divAssign(other: Amount) {
        value /= other.value
    }
    override fun toString(): String {
        return "$value ${currency.symbol}"
    }
    fun negate(): Amount {
        return Amount(value.negate(), currency)
    }
    fun isNegative(): Boolean {
        return value < BigDecimal.ZERO
    }
    companion object {
        fun fromString(representation: String, currency: Currency = Currency.EUR): Amount {
            return try {
                val amountAsBigDecimal = BigDecimal(representation)
                Amount(amountAsBigDecimal, currency)
            }catch(e: NumberFormatException) {
                throw InvalidMoneyFormatException(e.message!!)
            }
        }
    }

    fun <T> applyOnValue(function: (BigDecimal) -> T): T{
        return function(value)
    }

    fun toStringValue(): String {
        return value.toString()
    }
}

class InvalidMoneyFormatException(s: String): RuntimeException(s)

fun BigDecimal.toAmount(currency: Currency = Currency.EUR): Amount {
    return Amount(this, currency)
}

fun Int.toAmount(currency: Currency = Currency.EUR): Amount {
    return Amount(BigDecimal(this), currency)
}

fun Double.toAmount(currency: Currency = Currency.EUR): Amount {
    return Amount(BigDecimal(this), currency)
}

fun String.toAmount(currency: Currency = Currency.EUR): Amount {
    return Amount(BigDecimal(this), currency)
}

