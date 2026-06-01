package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.application.api.setup.BookletStateTestAdapter
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.util.UUID

private const val UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

/**
 * Validates the full diagnostic context chain:
 * RequestIdFilter → Spring Security → Bus (MDC) → Controller → ProblemDetailHandler → JSON response.
 *
 * When a user reports a bug, they copy the fields from the error toast and send them to support.
 * Support uses:
 *   - requestId → grep /var/log/jmanager/app.log to find all lines for that specific request
 *   - bookletId/transactionId → already in the same log lines via MDC (LoggingBus)
 *   - userId → look up the user and their data in the database
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class DiagnosticContextIntegrationTest(
    @LocalServerPort val port: Int,
    @Autowired val bookletStateAdapter: BookletStateTestAdapter,
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        bookletStateAdapter.clear()
    }

    @Nested
    inner class RequestIdInErrorResponse {

        @Test
        fun `authenticated 404 includes requestId, bookletId and userId`() {
            val bookletId = UUID.randomUUID().toString()

            Given {
                port(port)
                cookie("token", token)
            } When {
                get("/api/booklet/$bookletId")
            } Then {
                statusCode(404)
                body("properties.requestId", matchesPattern(UUID_PATTERN))
                body("properties.bookletId", matchesPattern(UUID_PATTERN))
                body("properties.userId", matchesPattern(UUID_PATTERN))
            }
        }

        @Test
        fun `authenticated 404 bookletId in response matches the id from the request path`() {
            val bookletId = UUID.randomUUID().toString()

            Given {
                port(port)
                cookie("token", token)
            } When {
                get("/api/booklet/$bookletId")
            } Then {
                statusCode(404)
                body("properties.bookletId", org.hamcrest.CoreMatchers.equalTo(bookletId))
            }
        }

        @Test
        fun `authenticated 404 userId in response matches the authenticated user`() {
            Given {
                port(port)
                cookie("token", token)
            } When {
                get("/api/booklet/${UUID.randomUUID()}")
            } Then {
                statusCode(404)
                body("properties.userId", org.hamcrest.CoreMatchers.equalTo(user!!.id.value.toString()))
            }
        }

        @Test
        fun `unauthenticated error includes requestId but no userId`() {
            Given {
                port(port)
                header("Content-Type", "application/json")
                body("""{"email":"ghost@example.com","password":"test"}""")
            } When {
                post("/api/user/auth")
            } Then {
                statusCode(404)
                body("properties.requestId", matchesPattern(UUID_PATTERN))
                body("properties.userId", nullValue())
            }
        }

        @Test
        fun `two distinct failing requests produce different requestIds`() {
            val firstId = Given {
                port(port)
                cookie("token", token)
            } When {
                get("/api/booklet/${UUID.randomUUID()}")
            } Then {
                statusCode(404)
            } Extract {
                path<String>("properties.requestId")
            }

            val secondId = Given {
                port(port)
                cookie("token", token)
            } When {
                get("/api/booklet/${UUID.randomUUID()}")
            } Then {
                statusCode(404)
            } Extract {
                path<String>("properties.requestId")
            }

            assertNotEquals(firstId, secondId)
        }
    }
}
