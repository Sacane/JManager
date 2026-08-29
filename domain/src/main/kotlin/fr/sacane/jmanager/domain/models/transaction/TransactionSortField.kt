package fr.sacane.jmanager.domain.models.transaction

/**
 * Field used to order every transaction of the period before pagination.
 *
 * [EXPENSE] and [INCOME] order by amount within their own kind and push the other kind
 * to the end of the list, regardless of the requested [TransactionSortDirection].
 */
enum class TransactionSortField {
    DATE,
    LABEL,
    EXPENSE,
    INCOME;
}
