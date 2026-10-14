package fr.sacane.jmanager.application.api.transaction

import fr.sacane.jmanager.application.api.InvalidRequestException
import fr.sacane.jmanager.application.api.currentUser
import fr.sacane.jmanager.application.api.requireSearchWithinLabelLength
import fr.sacane.jmanager.application.api.toDTO
import fr.sacane.jmanager.application.api.toHttpResponse
import fr.sacane.jmanager.application.bus.QueryBus
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.MonthMatches
import fr.sacane.jmanager.domain.models.RegularTransactionMatch
import fr.sacane.jmanager.domain.models.SearchedTransaction
import fr.sacane.jmanager.domain.models.TransactionSearchResult
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.input.transaction.SearchTransactionsQuery
import fr.sacane.jmanager.domain.utils.ResultState
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** Searches every booklet of the signed-in user by label, regular transactions folded (UX-22, part B). */
@RestController
@RequestMapping("api/transaction")
@Adapter(Side.APPLICATION)
class TransactionSearchController(
    private val queryBus: QueryBus,
) {
    @GetMapping("search")
    fun search(
        @RequestParam("q") search: String,
        @RequestParam(required = false, defaultValue = "12") months: Int,
        @RequestParam(required = false) bookletId: String?,
    ): ResponseEntity<TransactionSearchResponse> {
        requireSearchWithinLabelLength(search)
        return queryBus.dispatch(
            SearchTransactionsQuery(
                userId = UserId(currentUser.id),
                search = search,
                monthsBack = months,
                bookletId = bookletId?.let(::parseBookletId),
            ),
        ).map { it.toResponse() }.toHttpResponse()
    }

    private fun parseBookletId(raw: String): UUID = try {
        UUID.fromString(raw)
    } catch (_: IllegalArgumentException) {
        throw InvalidRequestException(ResultState.BAD_REQUEST.code, "L'identifiant de livret '$raw' est invalide")
    }
}

data class TransactionSearchResponse(
    val from: String,
    val to: String,
    val regulars: List<RegularTransactionMatchDTO>,
    val months: List<MonthMatchesDTO>,
    val hasOlderTransactions: Boolean,
)

data class RegularTransactionMatchDTO(
    val regularTransactionId: String,
    val label: String,
    val bookletId: String,
    val bookletLabel: String,
    val occurrenceCount: Int,
    val firstDate: String,
    val lastDate: String,
    val occurrences: List<SearchedTransactionDTO>,
)

/** [month] as "yyyy-MM". */
data class MonthMatchesDTO(val month: String, val transactions: List<SearchedTransactionDTO>)

data class SearchedTransactionDTO(
    val transaction: TransactionResult,
    val bookletId: String,
    val bookletLabel: String,
)

private fun TransactionSearchResult.toResponse() = TransactionSearchResponse(
    from = from.toString(),
    to = to.toString(),
    regulars = regulars.map { it.toDTO() },
    months = months.map { it.toDTO() },
    hasOlderTransactions = hasOlderTransactions,
)

private fun RegularTransactionMatch.toDTO() = RegularTransactionMatchDTO(
    regularTransactionId = regularTransactionId.value,
    label = label,
    bookletId = bookletId.toString(),
    bookletLabel = bookletLabel,
    occurrenceCount = occurrences.size,
    firstDate = firstDate.toString(),
    lastDate = lastDate.toString(),
    occurrences = occurrences.map { it.toDTO() },
)

private fun MonthMatches.toDTO() = MonthMatchesDTO(month = month.toString(), transactions = transactions.map { it.toDTO() })

private fun SearchedTransaction.toDTO() = SearchedTransactionDTO(
    transaction = transaction.toDTO(),
    bookletId = bookletId.toString(),
    bookletLabel = bookletLabel,
)
