package fr.sacane.jmanager.application.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.application.api.session.LoginRateLimiter
import fr.sacane.jmanager.application.api.session.UserPasswordDTO
import fr.sacane.jmanager.application.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource

// Failures allowed by LoginRateLimiter before it answers 429.
private const val MAX_FAILURES = 5

// Documentation addresses (RFC 5737): never a real visitor.
private const val CLIENT_A = "203.0.113.10"
private const val CLIENT_B = "203.0.113.11"
private const val UNKNOWN_ADDRESS = "nobody@example.com"

// What the limiter sees when the test client connects directly to the server.
private val LOOPBACK_KEYS = listOf("127.0.0.1", "0:0:0:0:0:0:0:1")

/**
 * Runs against a real embedded server: the client address comes from Tomcat's handling of the
 * forwarded header, which MockMvc bypasses.
 *
 * Production sits behind nginx, which appends the address it saw to whatever the client sent in
 * `X-Forwarded-For` (`$proxy_add_x_forwarded_for`). Without honouring that header the limiter keys every
 * visitor on the proxy's address, so five failures from anyone lock sign-in for everyone.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class LoginRateLimitTest(
    @LocalServerPort private val port: Int,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val userRepository: UserPostgresRepository,
    @Autowired private val bookletStateTestAdapter: BookletStateTestAdapter,
    @Autowired private val loginRateLimiter: LoginRateLimiter,
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        // The limiter is a singleton shared by every test class reusing this Spring context.
        (LOOPBACK_KEYS + CLIENT_A + CLIENT_B).forEach(loginRateLimiter::clearAttempts)
        bookletStateTestAdapter.clear()
        userRepository.deleteAll()
    }

    @Test
    fun `keys sign-in failures on the client address forwarded by the proxy`() {
        repeat(MAX_FAILURES) { assertEquals(404, failedSignIn(forwardedFor = CLIENT_A)) }

        assertEquals(429, failedSignIn(forwardedFor = CLIENT_A))
        assertEquals(404, failedSignIn(forwardedFor = CLIENT_B))
    }

    @Test
    fun `ignores addresses a client prepends to the forwarded header`() {
        repeat(MAX_FAILURES) { attempt ->
            assertEquals(404, failedSignIn(forwardedFor = "198.51.100.$attempt, $CLIENT_A"))
        }

        assertEquals(429, failedSignIn(forwardedFor = "198.51.100.99, $CLIENT_A"))
        assertEquals(404, failedSignIn(forwardedFor = "198.51.100.1, $CLIENT_B"))
    }

    @Test
    fun `keys sign-in failures on the connection address when no header is forwarded`() {
        repeat(MAX_FAILURES) { assertEquals(404, failedSignIn()) }

        assertEquals(429, failedSignIn())
    }

    // An unknown address answers 404 and counts as a failure, without paying for a BCrypt check.
    private fun failedSignIn(forwardedFor: String? = null): Int {
        val response: Response = Given {
            port(port)
            header("Content-Type", "application/json")
            if (forwardedFor != null) header("X-Forwarded-For", forwardedFor)
            body(objectMapper.writeValueAsString(UserPasswordDTO(email = UNKNOWN_ADDRESS, password = "wrong")))
        } When {
            post("/api/user/auth")
        }
        return response.statusCode
    }
}
