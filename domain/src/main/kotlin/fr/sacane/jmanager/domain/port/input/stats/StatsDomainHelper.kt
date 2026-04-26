package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import java.time.LocalDate
import java.util.UUID

internal fun <S> statsDomainFailure(state: ResultState, detail: String, key: String): Result<S> {
    return failure(state, DomainError(state.code, key, detail))
}

internal fun validateDateRange(startDate: LocalDate?, endDate: LocalDate?, keyPrefix: String): Pair<String, String>? {
    if ((startDate == null) != (endDate == null)) {
        return Pair(
            "La date de début et la date de fin doivent être fournies ensemble",
            "$keyPrefix.invalid_partial_date_range"
        )
    }

    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
        return Pair(
            "La date de début doit être antérieure à la date de fin",
            "$keyPrefix.invalid_date_range"
        )
    }

    return null
}

internal fun <S> withScopedBooklets(
    bookletRepository: BookletRepository,
    userId: UserId,
    bookletId: UUID?,
    onSuccess: (List<Booklet>) -> Result<S>
): Result<S> {
    if (bookletId == null) {
        return onSuccess(bookletRepository.findBookletsForUser(userId))
    }

    val booklet = bookletRepository.findBookletByIdWithTransactions(bookletId)
        ?: return statsDomainFailure(
            ResultState.BOOKLET_NOT_FOUND,
            "Le livret $bookletId est introuvable",
            "domain.stats.booklet_not_found"
        )

    if (booklet.owner?.id != userId) {
        return statsDomainFailure(
            ResultState.FORBIDDEN,
            "Vous n'avez pas accès à ce livret",
            "domain.stats.forbidden"
        )
    }

    return onSuccess(listOf(booklet))
}
