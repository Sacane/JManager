package fr.sacane.jmanager.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.infrastructure.api.session.RegisteredUserDTO
import fr.sacane.jmanager.infrastructure.api.session.UserPasswordDTO
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.AfterEach
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
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val userRepository: UserPostgresRepository
): AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        userRepository.deleteAll()
    }

    @Nested
    inner class LoginEndpointTest {
        @Test
        fun `Login a user must return 200`() {
            Given {
                port(port)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(UserPasswordDTO(user!!.username, "test")))
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
                cookie("token", token)
            } When {
                post("/api/user/logout")
            } Then {
                statusCode(200)
            }
        }
    }
//    @Nested
//    inner class CreateUserEndpointTest {
//        @Test
//        fun `Create a user must return 200`() {
//            Given {
//                port(port)
//                header("Content-Type", "application/json")
//                body(objectMapper.writeValueAsString(RegisteredUserDTO("test2", "test2", "test2")))
//            } When {
//                post("/api/user/create")
//            } Then {
//                statusCode(200)
//            }
//        }
//        @Test
//        fun `Create a user that has not the same password and confirm password must return 401`() {
//            Given {
//                port(port)
//                header("Content-Type", "application/json")
//                body(objectMapper.writeValueAsString(RegisteredUserDTO("test", "test", "test2")))
//            } When {
//                post("/api/user/create")
//            } Then {
//                statusCode(401)
//            }
//        }
//    }
}