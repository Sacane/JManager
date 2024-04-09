package fr.sacane.jmanager.domain.models

import java.math.BigDecimal

class Amount(private var amount: BigDecimal, private val currency: String = "€") {

    init{
        check(amount.scale() <= 2){
            "Expected scale at most 2, get ${amount.scale()}"
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
    companion object {
        fun fromString(representation: String): Amount {
            val regex = """([\d.]+) ([^\d.]+)""".toRegex()
            val matchResult = regex.find(representation) ?: throw InvalidMoneyFormatException("The amount format is not valid")
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
}

class InvalidMoneyFormatException(s: String): RuntimeException(s)

fun BigDecimal.toAmount(currency: String = "€"): Amount {
    return Amount(this, currency)
}

fun Int.toAmount(currency: String = "€"): Amount {
    return Amount(BigDecimal(this), currency)
}
fun Double.toAmount(currency: String = "€"): Amount {
    return Amount(BigDecimal(this), currency)
}