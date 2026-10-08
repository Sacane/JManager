package fr.sacane.jmanager.application.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.application.api.session.ForceChangePasswordDTO
import fr.sacane.jmanager.application.api.session.RecordConsentDTO
import fr.sacane.jmanager.domain.port.input.user.LoginCommand
import fr.sacane.jmanager.domain.port.input.user.LoginUseCase
import fr.sacane.jmanager.domain.port.output.Hasher
import fr.sacane.jmanager.infrastructure.spi.adapters.UserRepositoryJpaAdapter
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource

private const val CONSENT_REQUIRED = "domain.user.pending_step.consent_required"
private const val PASSWORD_CHANGE_REQUIRED = "domain.user.pending_step.password_change_required"
private const val TEMPORARY_PASSWORD = "temporary-password"
private const val NEW_PASSWORD = "new-password-123"

/**
 * An admin-created account must accept the terms and replace its temporary password before using the
 * application. The client shows the screens; the API is what actually holds the account back.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class PendingAccountStepApiTest(
    @LocalServerPort private val port: Int,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val userRepositoryJpaAdapter: UserRepositoryJpaAdapter,
    @Autowired private val loginUseCase: LoginUseCase,
    @Autowired private val hasher: Hasher,
) : AuthenticatedUserTest() {

    @Test
    fun `shouldRefuseTheApplication_whenAccountHasNotConsented`() {
        val pending = adminCreatedAccount()

        Given {
            port(port)
            cookie("token", pending)
        } When {
            get("/api/user/settings")
        } Then {
            statusCode(403)
            body("errorKey", equalTo(CONSENT_REQUIRED))
        }
    }

    @Test
    fun `shouldRefuseTheApplication_whenAccountConsentedButKeepsItsTemporaryPassword`() {
        val pending = adminCreatedAccount()
        consent(pending)

        Given {
            port(port)
            cookie("token", pending)
        } When {
            get("/api/user/settings")
        } Then {
            statusCode(403)
            body("errorKey", equalTo(PASSWORD_CHANGE_REQUIRED))
        }
    }

    @Test
    fun `shouldLetThePendingAccountReadItsStatusAndCompleteEachStep`() {
        val pending = adminCreatedAccount()

        assertThat(status(pending, "/api/user/me")).isEqualTo(200)
        assertThat(consent(pending)).isEqualTo(204)
        val renewed = Given {
            port(port)
            cookie("token", pending)
            header("Content-Type", "application/json")
            body(objectMapper.writeValueAsString(ForceChangePasswordDTO(NEW_PASSWORD, NEW_PASSWORD)))
        } When {
            post("/api/user/password/force")
        } Then {
            statusCode(204)
        } Extract { cookie("token") }

        assertThat(status(renewed, "/api/user/settings")).isEqualTo(200)
    }

    @Test
    fun `shouldLetThePendingAccountReachThePublicApi`() {
        val pending = adminCreatedAccount()

        assertThat(status(pending, "/api/feature-flags")).isEqualTo(200)
        assertThat(status(pending, "/api/password-policy")).isEqualTo(200)
    }

    @Test
    fun `shouldLetThePendingAccountSignOut`() {
        val pending = adminCreatedAccount()

        val signOut = Given {
            port(port)
            cookie("token", pending)
        } When {
            post("/api/user/logout")
        } Extract { statusCode() }

        assertThat(signOut).isLessThan(300)
    }

    // The right to erasure does not wait for the terms to be accepted.
    @Test
    fun `shouldLetThePendingAccountEraseItself`() {
        val pending = adminCreatedAccount()

        val erasure = Given {
            port(port)
            cookie("token", pending)
        } When {
            delete("/api/user/me")
        } Extract { statusCode() }

        assertThat(erasure).isEqualTo(204)
    }

    @Test
    fun `shouldServeTheApplication_whenAccountHasNoPendingStep`() {
        assertThat(status(token, "/api/user/settings")).isEqualTo(200)
    }

    private fun adminCreatedAccount(): String {
        userRepositoryJpaAdapter.register(
            username = "pending-user",
            password = hasher.hash(TEMPORARY_PASSWORD),
            roles = emptySet(),
            email = "pending-user@example.com",
            mustChangePassword = true,
        )
        return loginUseCase.handle(LoginCommand("pending-user@example.com", TEMPORARY_PASSWORD))
            .mapNotNullOrFailure()!!.token
    }

    private fun consent(accessToken: String): Int = Given {
        port(port)
        cookie("token", accessToken)
        header("Content-Type", "application/json")
        body(objectMapper.writeValueAsString(RecordConsentDTO(tosAccepted = true, tosVersion = "1.0", privacyAccepted = true)))
    } When {
        post("/api/user/consent")
    } Extract { statusCode() }

    private fun status(accessToken: String, path: String): Int = Given {
        port(port)
        cookie("token", accessToken)
    } When {
        get(path)
    } Extract { statusCode() }
}
