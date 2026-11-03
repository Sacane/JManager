package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.domain.utils.ResultState
import java.util.UUID

/** Reads a booklet identifier sent by the client: anything but a UUID is a bad request. */
internal fun parseBookletId(bookletId: String): UUID = try {
    UUID.fromString(bookletId)
} catch (_: IllegalArgumentException) {
    throw InvalidRequestException(
        ResultState.INVALID.code,
        "L'identifiant de livret '$bookletId' est invalide",
        "api.booklet.invalid_id",
    )
}
