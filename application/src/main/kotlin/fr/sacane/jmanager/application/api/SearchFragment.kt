package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.domain.utils.ResultState

/** Same bound as a transaction label: a longer fragment of label can match nothing. */
private const val MAX_SEARCH_LENGTH = 100

/** Refuses with a 400 a label search longer than a label can be. */
internal fun requireSearchWithinLabelLength(search: String?) {
    if (search != null && search.length > MAX_SEARCH_LENGTH) {
        throw InvalidRequestException(
            ResultState.BAD_REQUEST.code,
            "La recherche ne peut pas dépasser $MAX_SEARCH_LENGTH caractères",
        )
    }
}
