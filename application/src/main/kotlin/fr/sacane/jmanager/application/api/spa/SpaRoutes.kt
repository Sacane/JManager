package fr.sacane.jmanager.application.api.spa

/**
 * Which paths are pages of the Nuxt application, in one place shared by [SpaController], which forwards
 * them to the application shell, and by `SecurityConfig`, which lets anyone load them.
 *
 * A page path used to be listed in both classes; a page missing from either answered 401 when opened by
 * URL — the email verification page included. Rather than a list to keep in step with `client/pages`, a
 * page is now **any path that is not something else**: the API, the operational endpoints, the error
 * endpoint, the bundle's static directories, or a file (a last segment with an extension).
 *
 * The pages hold no data. Who may see what is decided by the API behind `/api` and by the client's route
 * middleware, never by the shell.
 */
object SpaRoutes {

    // The constraint regex may not contain a capture group (PathPattern refuses it), hence `(?:…)`.
    // First path segments that belong to something other than a page. The bundle's static directories are
    // served by the resource handlers of `WebConfig`; a controller mapping would otherwise win over them
    // (request mappings are consulted first) and answer a script request with HTML.
    private const val NOT_A_PAGE = "api|actuator|error|_nuxt|assets"

    /** A single-segment page such as `/login`. A dot marks a file (`/favicon.ico`), so it is not a page. */
    const val PAGE = "/{page:(?!(?:$NOT_A_PAGE)$)[^.]+}"

    /** A page below a first segment, such as `/booklet/{id}`. */
    const val NESTED_PAGE = "$PAGE/**"
}
