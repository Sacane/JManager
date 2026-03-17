package fr.sacane.jmanager.domain.utils

/**
 * Centralized application error code catalog used by backend and frontend.
 */
object ErrorCatalog {
    const val REQUEST_VALIDATION = 65
    const val REQUEST_TYPE_MISMATCH = 67
    const val REQUEST_DATE_TIME_INVALID = 68
    const val INTERNAL_UNEXPECTED = 111
    const val INVALID_CURRENCY = 144
    const val INVALID_UUID_NOT_FOUND = 404

    fun keyForCode(code: Int): String {
        val resultState = ResultState.entries.firstOrNull { state -> state.code == code }
        if (resultState != null) {
            return "result.${resultState.name.lowercase()}"
        }

        return when (code) {
            REQUEST_VALIDATION -> "request.validation"
            REQUEST_TYPE_MISMATCH -> "request.type_mismatch"
            REQUEST_DATE_TIME_INVALID -> "request.date_time_invalid"
            INTERNAL_UNEXPECTED -> "internal.unexpected"
            INVALID_CURRENCY -> "request.currency_invalid"
            INVALID_UUID_NOT_FOUND -> "resource.invalid_uuid_not_found"
            else -> "unknown.error"
        }
    }
}
