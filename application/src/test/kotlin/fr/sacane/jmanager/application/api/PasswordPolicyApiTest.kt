package fr.sacane.jmanager.application.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.application.api.session.ChangePasswordDTO
import fr.sacane.jmanager.application.api.session.RegisteredUserDTO
import fr.sacane.jmanager.application.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource

private const val POLICY_KEY = "domain.user.password.policy_violation"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class PasswordPolicyApiTest(
    @LocalServerPort private val port: Int,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val userRepository: UserPostgresRepository,
    @Autowired private val bookletStateTestAdapter: BookletStateTestAdapter,
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        bookletStateTestAdapter.clear()
        userRepository.deleteAll()
    }

    @Test
    fun `shouldAnswer400WithEveryUnmetRule_whenRegisteringWithTheAddressAsPassword`() {
        Given {
            port(port)
            header("Content-Type", "application/json")
            body(objectMapper.writeValueAsString(registration(password = "a@b.fr", email = "a@b.fr")))
        } When {
            post("/api/user/create")
        } Then {
            statusCode(400)
            body("errorKey", equalTo(POLICY_KEY))
            body("reasons", contains("too_short", "equals_email"))
        }
    }

    @Test
    fun `shouldAnswer400WithTheRule_whenChangingToAShortPassword`() {
        Given {
            port(port)
            header("Content-Type", "application/json")
            cookie("token", token)
            body(objectMapper.writeValueAsString(ChangePasswordDTO(TEST_USER_PASSWORD, "short-pass", "short-pass")))
        } When {
            patch("/api/user/password")
        } Then {
            statusCode(400)
            body("errorKey", equalTo(POLICY_KEY))
            body("reasons", contains("too_short"))
        }
    }

    // The screens show the rules before anyone types, including the sign-up screen: no session needed.
    @Test
    fun `shouldPublishThePolicy_withoutSigningIn`() {
        Given {
            port(port)
        } When {
            get("/api/password-policy")
        } Then {
            statusCode(200)
            body("minLength", equalTo(12))
            body("maxLength", equalTo(100))
            body("rules", contains("too_short", "too_long", "equals_email"))
        }
    }

    private fun registration(password: String, email: String) = RegisteredUserDTO(
        username = "newcomer",
        password = password,
        confirmPassword = password,
        email = email,
        tosAccepted = true,
        tosVersion = "1.0",
        privacyAccepted = true,
    )
}
