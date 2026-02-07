package fr.sacane.jmanager.domain.models

/**
 * Lightweight value object used by API to return balances without the transaction listing.
 */
data class BookletBalances(
    val label: String,
    val realSold: Amount,
    val previewSold: Amount
)

