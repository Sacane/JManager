package fr.sacane.jmanager.domain.models

import java.math.BigDecimal
import java.math.RoundingMode

data class Amount(var amount: BigDecimal, val currency: Currency = Currency.EUR) {
    constructor(longAmount: Long) : this(BigDecimal(longAmount), currency =  Currency.EUR)

    init{
        if(amount.scale() <= 2){
            amount = amount.setScale(2, RoundingMode.UP)
        }
    }
    operator fun plusAssign(other: Amount) {
        amount += other.amount
    }
    operator fun minusAssign(other: Amount) {
        amount -= other.amount
    }
    operator fun plus(other: Amount): Amount {
        return Amount(other.amount + amount, currency)
    }
    operator fun minus(other: Amount): Amount{
        return Amount(amount - other.amount, currency)
    }
    operator fun timesAssign(other: Amount) {
        amount *= other.amount
    }
    operator fun divAssign(other: Amount) {
        amount /= other.amount
    }
    override fun toString(): String {
        return "$amount ${currency.symbol}"
    }
    fun negate(): Amount {
        return Amount(amount.negate(), currency)
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
        return function(amount)
    }

    fun toStringValue(): String {
        return amount.toString()
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
