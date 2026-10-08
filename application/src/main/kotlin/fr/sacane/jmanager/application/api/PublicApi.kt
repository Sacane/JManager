package fr.sacane.jmanager.application.api

/** API paths open to anyone, signed in or not. */
object PublicApi {
    val PATHS: List<String> = listOf(
        "/api/feature-flags",
        "/api/user/create",
        "/api/user/auth",
        "/api/user/auth/refresh/**",
        "/api/verify-email",
        "/api/password-policy",
    )
}
