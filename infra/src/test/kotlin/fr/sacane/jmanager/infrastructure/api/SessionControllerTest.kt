package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.infrastructure.api.session.UserPasswordDTO
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class SessionControllerTest(
    @LocalServerPort private val port: Int,
    @Autowired val objectMapper: ObjectMapper
): AuthenticatedUserTest() {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }
    }
    @Nested
    inner class LoginEndpointTest {
        @Test
        fun `Login a user must return 200`() {
            Given {
                port(port)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(UserPasswordDTO("test", "test")))
            } When {
                post("/api/user/auth")
            } Then {
                statusCode(200)
            }
        }
    }

    @Nested
    inner class LogoutEndpointTest {
        @Test
        fun `Request for logout a connected user must return 200`(){
            Given {
                port(port)
                header("Authorization", token)
            } When {
                post("/api/user/logout/${user!!.id.value}")
            } Then {
                statusCode(200)
            }
        }
    }
}