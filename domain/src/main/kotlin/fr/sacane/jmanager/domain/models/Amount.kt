package fr.sacane.jmanager.domain.models

import java.math.BigDecimal
import java.math.RoundingMode

class Amount(var amount: BigDecimal, val currency: String = "€") {
    constructor(longAmount: Long) : this(BigDecimal(longAmount), currency = "€")

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
        return "$amount $currency"
    }
    fun negate(): Amount {
        return Amount(amount.negate(), currency)
    }
    companion object {
        fun fromString(representation: String): Amount {
            val regex = """([\d.]+) ([^\d.]+)""".toRegex()
            val matchResult = regex.find(representation) ?: throw InvalidMoneyFormatException("The amount format is not valid : $representation")
            val (amount, foundCurrency) = matchResult.destructured
            if(foundCurrency.length > 1) throw InvalidMoneyFormatException("The length of the currency should exactly be equals to 1")
            return try {
                val amountAsBigDecimal = BigDecimal(amount)
                Amount(amountAsBigDecimal, foundCurrency)
            }catch(e: NumberFormatException) {
                throw InvalidMoneyFormatException(e.message!!)
            }
        }
    }

    override fun equals(other: Any?)
    : Boolean = other is Amount && amount == other.amount && currency == other.currency

    override fun hashCode(): Int {
        var result = amount.hashCode()
        result = 31 * result + currency.hashCode()
        return result

    }
    fun <T> applyOnValue(function: (BigDecimal) -> T): T{
        return function(amount)
    }

    fun toStringValue(): String {
        return amount.toString()
    }
}

class InvalidMoneyFormatException(s: String): RuntimeException(s)

fun BigDecimal.toAmount(    currency: String = "€"): Amount {
    return Amount(this, currency)
}

fun Int.toAmount(currency: String = "€"): Amount {
    return Amount(BigDecimal(this), currency)
}
fun Double.toAmount(currency: String = "€"): Amount {
    return Amount(BigDecimal(this), currency)
}
