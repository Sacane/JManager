package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.BookletSummary
import fr.sacane.jmanager.domain.models.LabelSearch
import fr.sacane.jmanager.domain.models.MonthMatches
import fr.sacane.jmanager.domain.models.RegularTransactionMatch
import fr.sacane.jmanager.domain.models.SearchedTransaction
import fr.sacane.jmanager.domain.models.TransactionSearchResult
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionQueryRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Searches the labels of the transactions of a user's booklets over the last [monthsBack] months,
 * the current one included; [bookletId] narrows it to one booklet of theirs.
 */
data class SearchTransactionsQuery(
    val userId: UserId,
    val search: String,
    val monthsBack: Int = 12,
    val bookletId: UUID? = null,
) : Query<TransactionSearchResult>

@Port(Side.APPLICATION)
interface SearchTransactionsUseCase : QueryHandler<SearchTransactionsQuery, TransactionSearchResult> {
    override val queryClass get() = SearchTransactionsQuery::class
}

/**
 * Matches labels with the same rule as the booklet view ([LabelSearch]), in memory over the window:
 * at most six booklets, twelve months by default. The occurrences of a regular transaction fold into
 * one entry per booklet, so years of a monthly rent stay one line; previsional occurrences excluded
 * from their month in the booklet view stay hidden.
 */
@DomainService
class SearchTransactionsService(
    private val bookletRepository: BookletRepository,
    private val transactionQueryRepository: TransactionQueryRepository,
    private val trackerRepository: RegularTransactionTrackerRepository,
    private val clock: Clock,
) : SearchTransactionsUseCase {

    companion object {
        const val MAX_MONTHS_BACK = 120
    }

    override fun handle(query: SearchTransactionsQuery): Result<TransactionSearchResult> {
        if (query.monthsBack !in 1..MAX_MONTHS_BACK) {
            return failure(
                ResultState.INVALID,
                DomainError(
                    ResultState.INVALID.code,
                    "domain.transaction.search.invalid_window",
                    "La recherche porte sur 1 à $MAX_MONTHS_BACK mois",
                ),
            )
        }

        val owned = bookletRepository.findBookletSummariesForUser(query.userId)
        val booklets = if (query.bookletId == null) owned else {
            owned.filter { it.id == query.bookletId }.ifEmpty {
                return failure(
                    ResultState.FORBIDDEN,
                    DomainError(ResultState.FORBIDDEN.code, "domain.transaction.search.forbidden_booklet", "Ce livret ne vous appartient pas"),
                )
            }
        }

        val currentMonth = YearMonth.now(clock)
        val from = currentMonth.minusMonths(query.monthsBack - 1L).atDay(1)
        val to = currentMonth.atEndOfMonth()
        val search = LabelSearch.of(query.search)
        val bookletIds = booklets.map { it.id }

        val found = if (search.isEmpty) emptyList() else booklets.flatMap { booklet ->
            visibleTransactions(booklet, from, to)
                .filter { search.matches(it.label) }
                .map { SearchedTransaction(it, booklet.id, booklet.label) }
        }
        val (fromRegulars, oneOffs) = found.partition { it.transaction.regularTransactionId != null }

        return success(
            TransactionSearchResult(
                from = from,
                to = to,
                regulars = fold(fromRegulars),
                months = byMonth(oneOffs),
                hasOlderTransactions = bookletIds.isNotEmpty() &&
                    transactionQueryRepository.findEarliestTransactionDate(bookletIds)?.isBefore(from) == true,
            ),
        )
    }

    // What the booklet view shows: a previsional occurrence removed from its month stays removed.
    private fun visibleTransactions(booklet: BookletSummary, from: LocalDate, to: LocalDate): List<Transaction> {
        val excludedMonths = trackerRepository.findAllTrackersForBooklet(booklet.id)
            .associate { it.regularTransactionId to it.excludedMonths }
        return transactionQueryRepository.findByBookletIdAndDateBetween(booklet.id, from, to).filter { transaction ->
            val regular = transaction.regularTransactionId
            !(transaction.isPreview && regular != null && excludedMonths[regular]?.contains(YearMonth.from(transaction.date)) == true)
        }
    }

    private fun fold(occurrences: List<SearchedTransaction>): List<RegularTransactionMatch> =
        occurrences
            .groupBy { it.transaction.regularTransactionId!! to it.bookletId }
            .map { (key, matches) ->
                RegularTransactionMatch(
                    regularTransactionId = key.first,
                    bookletId = key.second,
                    bookletLabel = matches.first().bookletLabel,
                    occurrences = matches.sortedWith(NEWEST_FIRST),
                )
            }
            .sortedByDescending { it.lastDate }

    private fun byMonth(transactions: List<SearchedTransaction>): List<MonthMatches> =
        transactions
            .groupBy { YearMonth.from(it.transaction.date) }
            .map { (month, matches) -> MonthMatches(month, matches.sortedWith(NEWEST_FIRST)) }
            .sortedByDescending { it.month }
}

private val NEWEST_FIRST: Comparator<SearchedTransaction> =
    compareByDescending<SearchedTransaction> { it.transaction.date }.thenByDescending { it.transaction.lastModified }
