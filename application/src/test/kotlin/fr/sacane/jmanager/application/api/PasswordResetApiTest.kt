package fr.sacane.jmanager.application.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.application.api.password.ConfirmPasswordResetDTO
import fr.sacane.jmanager.application.api.password.PasswordResetRequestDTO
import fr.sacane.jmanager.application.api.password.PasswordResetTokenDTO
import fr.sacane.jmanager.domain.models.PasswordResetToken
import fr.sacane.jmanager.domain.port.input.user.LoginCommand
import fr.sacane.jmanager.domain.port.input.user.LoginUseCase
import fr.sacane.jmanager.domain.port.output.repository.PasswordResetTokenRepository
import fr.sacane.jmanager.infrastructure.spi.adapters.UserRepositoryJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.PasswordResetTokenJpaRepository
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.Response
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

private const val REGISTERED = "test@example.com"
private const val NEW_PASSWORD = "brand-new-password"

// Every test speaks from its own documentation address (RFC 5737): the limiter is shared by the context.
private val nextClient = AtomicInteger(1)

/** Anonymous endpoints to request, check and confirm a password reset. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class PasswordResetApiTest(
    @LocalServerPort private val port: Int,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val tokenRepository: PasswordResetTokenRepository,
    @Autowired private val tokenJpaRepository: PasswordResetTokenJpaRepository,
    @Autowired private val loginUseCase: LoginUseCase,
    @Autowired private val userRepositoryJpaAdapter: UserRepositoryJpaAdapter,
) : AuthenticatedUserTest() {

    private val client = "203.0.113.${nextClient.getAndIncrement()}"

    @AfterEach
    fun cleanup() = tokenJpaRepository.deleteAll()

    @Test
    fun `shouldAnswerTheSameForARegisteredAndAnUnknownAddress`() {
        val registered = request(REGISTERED)
        val unknown = request("nobody@example.com")

        assertThat(registered.statusCode).isEqualTo(202)
        assertThat(unknown.statusCode).isEqualTo(202)
        assertThat(registered.body.asString()).isEqualTo(unknown.body.asString()).isEmpty()
        assertThat(tokenJpaRepository.findAll().map { it.userId }).containsExactly(user!!.id.value)
    }

    @Test
    fun `shouldReportAValidTokenAndKeepItUsable`() {
        val token = givenToken()

        assertThat(validate(token).statusCode).isEqualTo(204)
        assertThat(confirm(token).statusCode).isEqualTo(204)
    }

    @Test
    fun `shouldReportAStaleTokenWithItsReason`() {
        val expired = givenToken(expiresAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1))

        validate(expired).then().statusCode(400).body("errorKey", equalTo("domain.user.password_reset.token_expired"))
        validate("never-issued").then().statusCode(400).body("errorKey", equalTo("domain.user.password_reset.token_invalid"))
    }

    @Test
    fun `shouldLetTheUserSignInWithTheNewPassword_whenResetIsConfirmed`() {
        val token = givenToken()

        assertThat(confirm(token).statusCode).isEqualTo(204)

        assertThat(loginUseCase.handle(LoginCommand(REGISTERED, NEW_PASSWORD)).isSuccess()).isTrue()
        assertThat(loginUseCase.handle(LoginCommand(REGISTERED, TEST_USER_PASSWORD)).isFailure()).isTrue()
    }

    @Test
    fun `shouldKeepTheDomainErrorKeys_whenConfirmationIsRefused`() {
        val token = givenToken()

        confirm(token, confirmPassword = "something-else-entirely").then()
            .body("errorKey", equalTo("domain.user.password.mismatch"))
        confirm(token, newPassword = "short", confirmPassword = "short").then()
            .statusCode(400)
            .body("errorKey", equalTo("domain.user.password.policy_violation"))
    }

    // Counts requests, not sends: a 429 says nothing about whether the address is registered.
    // Its own addresses: the per-address count lasts an hour and the other tests use the shared ones.
    @Test
    fun `shouldLimitRequestsPerAddress_whateverTheClient`() {
        userRepositoryJpaAdapter.register("flooded", "irrelevant-hash", emptySet(), email = "flooded@example.com")
        repeat(3) { attempt -> assertThat(request("flooded@example.com", from = "198.51.100.${attempt + 1}").statusCode).isEqualTo(202) }
        repeat(3) { attempt -> assertThat(request("ghost@example.com", from = "198.51.100.${attempt + 10}").statusCode).isEqualTo(202) }

        assertThat(request("FLOODED@example.com", from = "198.51.100.50").statusCode).isEqualTo(429)
        assertThat(request("ghost@example.com", from = "198.51.100.51").statusCode).isEqualTo(429)
    }

    @Test
    fun `shouldLimitRequestsPerClient`() {
        repeat(5) { attempt -> assertThat(request("someone-$attempt@example.com").statusCode).isEqualTo(202) }

        assertThat(request("someone-else@example.com").statusCode).isEqualTo(429)
    }

    @Test
    fun `shouldLimitTokenChecksPerClient`() {
        repeat(5) { assertThat(validate("never-issued").statusCode).isEqualTo(400) }
        repeat(5) { assertThat(confirm("never-issued").statusCode).isEqualTo(400) }

        assertThat(validate("never-issued").statusCode).isEqualTo(429)
        assertThat(confirm("never-issued").statusCode).isEqualTo(429)
    }

    private fun givenToken(expiresAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30)): String {
        val raw = "raw-token-${System.nanoTime()}"
        tokenRepository.save(PasswordResetToken(PasswordResetToken.digestOf(raw), user!!.id, expiresAt))
        return raw
    }

    private fun request(email: String, from: String = client): Response = post("/api/password-reset/request", PasswordResetRequestDTO(email), from)

    private fun validate(token: String): Response = post("/api/password-reset/validate", PasswordResetTokenDTO(token), client)

    private fun confirm(token: String, newPassword: String = NEW_PASSWORD, confirmPassword: String = newPassword): Response =
        post("/api/password-reset/confirm", ConfirmPasswordResetDTO(token, newPassword, confirmPassword), client)

    private fun post(path: String, body: Any, from: String): Response = Given {
        port(port)
        header("Content-Type", "application/json")
        header("X-Forwarded-For", from)
        body(objectMapper.writeValueAsString(body))
    } When {
        post(path)
    } Extract { response() }
}
