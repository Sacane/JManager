package fr.sacane.jmanager.infrastructure

fun extractToken(authorization: String): String = authorization.replace("Bearer ", "")

object Environment {
    const val DEFAULT_TOKEN_LIFETIME_IN_HOURS = 1L
    const val DEFAULT_REFRESH_TOKEN_LIFETIME_IN_DAYS = 7L
}