package fr.sacane.jmanager.domain.models

enum class Currency (
    val symbol: String
) {
    EUR("€"),
    USD("$"),
    GPB("£");

    companion object {
        fun asList() : List<Currency> = listOf(
            EUR,
            GPB,
            USD
        )
        fun fromSymbolString(s: String) : Currency
            = asList().find { it.symbol == s } ?: throw InvalidCurrencyException("The currency $s does not exists")
    }
}

class InvalidCurrencyException(message: String) : RuntimeException(message)

fun String.asCurrency() : Currency
    = Currency.fromSymbolString(this)