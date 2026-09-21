package fr.sacane.jmanager.application.api.spa

import fr.sacane.jmanager.application.api.AuthenticatedUserTest
import fr.sacane.jmanager.application.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource

// Stands in for the Nuxt bundle: application/src/test/resources/static/index.html.
private const val SHELL_MARKER = "<!-- spa-shell -->"

// Pages a browser opens by URL: from an email, a bookmark, a new tab or a reload. Includes the pages
// that used to answer 401 (verify-email, privacy, terms, consent, force-password-change, settings), the
// two pages UX-21 adds, and one path no page owns, which the client answers with its own 404 screen.
private val PAGES_OPENED_DIRECTLY = listOf(
    "/",
    "/login",
    "/verify-email?token=abc123",
    "/forgot-password",
    "/reset-password",
    "/privacy",
    "/terms",
    "/consent",
    "/force-password-change",
    "/settings",
    "/dashboard",
    "/booklet",
    "/booklet/3f2b9c4e-1111-4111-8111-000000000000",
    "/regular-transaction",
    "/tag",
    "/admin",
    "/admin/users",
    "/no-such-page",
)

// What must keep answering for itself instead of serving the shell: an anonymous browser gets none of it.
private val PATHS_THAT_ARE_NOT_PAGES = listOf(
    "/api/no-such-endpoint",
    "/api/user/me",
    "/actuator/env",
    "/actuator/heapdump",
    "/_nuxt/missing.js",
    "/assets/missing.png",
    "/favicon-missing.ico",
)

/**
 * The application serves the Nuxt bundle itself, so it must answer a page URL with the shell (index.html)
 * whatever the page, for anyone: the pages hold no data, the API behind `/api` is what is protected.
 *
 * Until now a page was reachable only if it appeared both in `SecurityConfig` and in `SpaController`; in
 * production every page missing from one of them answered 401 when opened by URL, the email verification
 * page included. See docs/bugs/spa-pages-401-on-direct-load/.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class SpaRoutingTest(
    @LocalServerPort private val port: Int,
    @Autowired private val userRepository: UserPostgresRepository,
    @Autowired private val bookletStateTestAdapter: BookletStateTestAdapter,
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        bookletStateTestAdapter.clear()
        userRepository.deleteAll()
    }

    @Test
    fun `serves the application shell for every page opened by URL, without signing in`() {
        val statuses = PAGES_OPENED_DIRECTLY.associateWith { browserGet(it).statusCode }

        assertEquals(PAGES_OPENED_DIRECTLY.associateWith { 200 }, statuses)
    }

    @Test
    fun `the page body is the application shell`() {
        val withoutShell = PAGES_OPENED_DIRECTLY.filterNot { browserGet(it).asString().contains(SHELL_MARKER) }

        assertEquals(emptyList<String>(), withoutShell)
    }

    @Test
    fun `never serves the application shell for a path that is not a page`() {
        val servedShell = PATHS_THAT_ARE_NOT_PAGES.filter { browserGet(it).asString().contains(SHELL_MARKER) }

        assertEquals(emptyList<String>(), servedShell)
    }

    @Test
    fun `still refuses an anonymous request to a protected endpoint`() {
        assertEquals(401, browserGet("/api/user/me").statusCode)
        assertEquals(401, browserGet("/api/booklet").statusCode)
    }

    @Test
    fun `keeps the operational endpoints closed except health and info`() {
        assertFalse(browserGet("/actuator/env").statusCode in 200..299)
        assertFalse(browserGet("/actuator/heapdump").statusCode in 200..299)
    }

    // What a browser sends when someone clicks a link: it asks for HTML, and carries no session.
    private fun browserGet(path: String): Response =
        Given {
            port(port)
            header("Accept", "text/html,application/xhtml+xml")
            redirects().follow(false)
        } When {
            get(path)
        }
}
