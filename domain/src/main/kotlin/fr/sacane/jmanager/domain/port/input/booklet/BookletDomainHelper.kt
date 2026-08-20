package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal fun <S> bookletDomainFailure(state: ResultState, detail: String, key: String): Result<S> {
    return failure(state, DomainError(state.code, key, detail))
}

internal fun userOwnsBooklet(bookletRepository: BookletRepository, userId: UserId, bookletId: UUID): Boolean {
    val userBooklets = bookletRepository.findBookletsForUser(userId)
    return userBooklets.any { it.id == bookletId }
}

/**
 * Tells whether the given month is currently excluded for a regular transaction in a booklet,
 * i.e. whether the user deleted that month's occurrences and could restore them.
 */
internal fun RegularTransactionTrackerRepository.excludesMonth(
    regularTransactionId: RegularTransactionId,
    bookletId: UUID,
    yearMonth: YearMonth
): Boolean = findTracker(regularTransactionId, bookletId)?.excludedMonths?.contains(yearMonth) == true

internal fun monthDateBounds(start: YearMonth, end: YearMonth): Pair<LocalDate, LocalDate> {
    val from = minOf(start, end).atDay(1)
    val to = maxOf(start, end).atEndOfMonth()
    return from to to
}

internal fun calculatePrevisionalSold(
    regularTransactionGeneratorService: RegularTransactionGenerator,
    booklet: Booklet,
    regularTransactions: List<RegularTransaction>,
    rangeStartDate: LocalDate,
    rangeEndDate: LocalDate,
    allPhysicalTransactionsForDedup: List<Transaction> = booklet.transactions
): Amount {
    val allTransactions = booklet.transactions

    val relevantPreviewTransactions = allTransactions
        .filter { tx -> !tx.date.isBefore(rangeStartDate) && !tx.date.isAfter(rangeEndDate) }
        .filter { it.isPreview }

    val relevantPhysicalTransactionsForDedup = allPhysicalTransactionsForDedup
        .filter { tx -> !tx.date.isBefore(rangeStartDate) && !tx.date.isAfter(rangeEndDate) }

    val virtualTransactions = regularTransactionGeneratorService.calculateVirtualTransactions(
        booklet.id!!,
        regularTransactions,
        rangeStartDate.month,
        rangeStartDate.year,
        rangeEndDate.month,
        rangeEndDate.year,
        existingPhysicalTransactions = relevantPhysicalTransactionsForDedup
    ).filter { tx -> !tx.date.isBefore(rangeStartDate) && !tx.date.isAfter(rangeEndDate) }

    val allRelevantTransactions = relevantPreviewTransactions + virtualTransactions

    val totalAmount = allRelevantTransactions.fold(BigDecimal.ZERO) { acc, transaction ->
        val value = transaction.amount.value.abs()
        if (transaction.isIncome) {
            acc.add(value)
        } else {
            acc.subtract(value)
        }
    }

    return Amount(booklet.amount.value.add(totalAmount))
}
