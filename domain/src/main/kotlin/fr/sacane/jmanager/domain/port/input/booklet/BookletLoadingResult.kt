package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import java.util.UUID

/**
 * @property currentTransactions Confirmed transactions of the requested page.
 * @property previsionalTransactions Previsional and virtual transactions of the requested page.
 * @property orderedTransactions Every transaction of the requested page, in display order. Callers that
 *           render the page as a single list must use this property: rebuilding it from
 *           [currentTransactions] and [previsionalTransactions] discards the requested date ordering.
 * @property balanceAfter The balance each confirmed transaction of the whole period leaves, by id, as on a
 *           bank statement (UX-25). Previsional transactions have none. Unaffected by paging, sorting and search.
 */
data class BookletLoadingResult(
    val label: String,
    val currentTransactions: List<Transaction>,
    val previsionalTransactions: List<Transaction>,
    val regularTransactions: List<RegularTransaction>,
    val realSold: Amount,
    val previsionalSold: Amount,
    val hasRegenerableTransactions: Boolean = false,
    val pageNumber: Int = 0,
    val pageSize: Int = 10,
    val totalElements: Long = 0L,
    val totalPages: Int = 1,
    val orderedTransactions: List<Transaction> = currentTransactions + previsionalTransactions,
    val balanceAfter: Map<UUID, Amount> = emptyMap(),
)
