package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** A booklet named, without its transactions. */
data class BookletSummary(val id: UUID, val label: String)

/** A transaction found by a search, with the booklet it belongs to. */
data class SearchedTransaction(
    val transaction: Transaction,
    val bookletId: UUID,
    val bookletLabel: String,
)

/** Every occurrence found of one regular transaction in one booklet, newest first. */
data class RegularTransactionMatch(
    val regularTransactionId: RegularTransactionId,
    val bookletId: UUID,
    val bookletLabel: String,
    val occurrences: List<SearchedTransaction>,
) {
    /** The label of the latest occurrence: the one the user knows it by today. */
    val label: String get() = occurrences.first().transaction.label
    val firstDate: LocalDate get() = occurrences.last().transaction.date
    val lastDate: LocalDate get() = occurrences.first().transaction.date
}

/** The one-off transactions found in a month, newest first. */
data class MonthMatches(val month: YearMonth, val transactions: List<SearchedTransaction>)

/**
 * What a search over every booklet found between [from] and [to]: regular transactions folded into one
 * entry each, the others grouped by month, newest first.
 */
data class TransactionSearchResult(
    val from: LocalDate,
    val to: LocalDate,
    val regulars: List<RegularTransactionMatch>,
    val months: List<MonthMatches>,
    /** Whether any transaction, matching or not, predates [from]: searching further back may find more. */
    val hasOlderTransactions: Boolean,
)
