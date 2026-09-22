package fr.sacane.jmanager.application.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.application.api.session.ChangePasswordDTO
import fr.sacane.jmanager.application.api.session.ForceChangePasswordDTO
import fr.sacane.jmanager.domain.port.input.user.LoginCommand
import fr.sacane.jmanager.domain.port.input.user.LoginUseCase
import fr.sacane.jmanager.domain.port.output.Hasher
import fr.sacane.jmanager.infrastructure.spi.adapters.UserRepositoryJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.Response
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val NEW_PASSWORD = "new-password-123"

/** A new password ends the sessions opened with the old one, and keeps the device that changed it signed in. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class CredentialRevocationApiTest(
    @LocalServerPort private val port: Int,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val userPostgresRepository: UserPostgresRepository,
    @Autowired private val userRepositoryJpaAdapter: UserRepositoryJpaAdapter,
    @Autowired private val loginUseCase: LoginUseCase,
    @Autowired private val hasher: Hasher,
    @Value("\${auth.secret}") private val secret: String,
) : AuthenticatedUserTest() {

    @Test
    fun `shouldRefuseAccessToken_whenIssuedBeforeTheCredentialsChanged`() {
        credentialsChangedAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1))

        assertThat(me(token)).isEqualTo(401)
    }

    @Test
    fun `shouldRefuseAccessTokenWithoutIssueTime_whenCredentialsChanged`() {
        credentialsChangedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1))

        assertThat(me(tokenWithoutIssueTime())).isEqualTo(401)
    }

    @Test
    fun `shouldAcceptAccessTokenWithoutIssueTime_whenCredentialsNeverChanged`() {
        assertThat(me(tokenWithoutIssueTime())).isEqualTo(200)
    }

    @Test
    fun `shouldKeepTheDeviceSignedInAndRevokeOldRefreshToken_whenPasswordIsChanged`() {
        val response = Given {
            port(port)
            cookie("token", token)
            header("Content-Type", "application/json")
            body(objectMapper.writeValueAsString(ChangePasswordDTO(TEST_USER_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)))
        } When {
            patch("/api/user/password")
        } Then {
            statusCode(204)
        } Extract { response() }

        assertTheDeviceWasSignedInAgain(response, previousRefreshToken = refreshToken)
        assertThat(refresh(refreshToken)).isEqualTo(401)
    }

    @Test
    fun `shouldKeepTheDeviceSignedIn_whenForcedChangeCompletes`() {
        userRepositoryJpaAdapter.register(
            username = "forced-user",
            password = hasher.hash("temporary-password"),
            roles = emptySet(),
            email = "forced-user@example.com",
            mustChangePassword = true,
        )
        val forcedSession = loginUseCase.handle(LoginCommand("forced-user@example.com", "temporary-password"))
            .mapNotNullOrFailure()!!

        val response = Given {
            port(port)
            cookie("token", forcedSession.token)
            header("Content-Type", "application/json")
            body(objectMapper.writeValueAsString(ForceChangePasswordDTO(NEW_PASSWORD, NEW_PASSWORD)))
        } When {
            post("/api/user/password/force")
        } Then {
            statusCode(204)
        } Extract { response() }

        assertTheDeviceWasSignedInAgain(response, previousRefreshToken = forcedSession.refreshToken.toString())
    }

    // Compares refresh tokens: they are random, whereas two access tokens issued in the same second are identical.
    private fun assertTheDeviceWasSignedInAgain(response: Response, previousRefreshToken: String) {
        val newToken = response.cookie("token")
        val newRefreshToken = response.cookie("refresh_token")
        assertNotNull(newToken)
        assertNotNull(newRefreshToken)
        assertNotEquals(previousRefreshToken, newRefreshToken)
        assertThat(me(newToken)).isEqualTo(200)
        assertThat(refresh(newRefreshToken)).isEqualTo(200)
    }

    private fun credentialsChangedAt(at: LocalDateTime) {
        val resource = userPostgresRepository.findById(user!!.id.value!!).orElseThrow()
        resource.credentialsChangedAt = at
        userPostgresRepository.save(resource)
    }

    // Shaped and signed like every token issued before the issue time was recorded: no "iat" claim.
    private fun tokenWithoutIssueTime(): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val expiresAt = Instant.now().plusSeconds(60).epochSecond
        val header = encoder.encodeToString("""{"alg":"HS256"}""".toByteArray())
        val payload = encoder.encodeToString(
            """{"sub":"${user!!.id.value}","exp":$expiresAt,"username":"test","USER":true}""".toByteArray(),
        )
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secret.toByteArray(), "HmacSHA256")) }
        val signature = encoder.encodeToString(mac.doFinal("$header.$payload".toByteArray()))
        return "$header.$payload.$signature"
    }

    private fun me(accessToken: String): Int = Given {
        port(port)
        cookie("token", accessToken)
    } When {
        get("/api/user/me")
    } Extract { statusCode() }

    private fun refresh(refreshToken: String): Int = Given {
        port(port)
        cookie("refresh_token", refreshToken)
    } When {
        post("/api/user/auth/refresh")
    } Extract { statusCode() }

    private fun assertThat(status: Int) = org.assertj.core.api.Assertions.assertThat(status)
}
