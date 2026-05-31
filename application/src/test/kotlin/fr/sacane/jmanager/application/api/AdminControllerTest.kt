package fr.sacane.jmanager.application.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.application.api.admin.AdminCreateUserRequest
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.port.input.user.*
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class AdminControllerTest(
    @LocalServerPort private val port: Int,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val userRepository: UserPostgresRepository,
    @Autowired private val registerUserUseCase: RegisterUserUseCase,
    @Autowired private val loginUseCase: LoginUseCase,
    @Autowired private val createAdminIfNotExistsUseCase: CreateAdminIfNotExistsUseCase
) : AuthenticatedUserTest() {

    private lateinit var adminToken: String

    @BeforeEach
    fun setupAdmin() {
        createAdminIfNotExistsUseCase.handle(CreateAdminIfNotExistsCommand("admin2", "admin123")).onSuccess {
            loginUseCase.handle(LoginCommand("admin2", "admin123")).onSuccess { loginResult ->
                adminToken = loginResult.token
            }
        }
    }

    @AfterEach
    fun clear() {
        val allUsers = userRepository.findAll()
        allUsers.filter { it.username != "admin2" && it.username != "test" }.forEach {
            userRepository.delete(it)
        }
    }

    private fun consentCommand(username: String, email: String) = RegisterUserCommand(
        username = username, password = "test", confirmPassword = "test", email = email,
        tosAcceptedAt = LocalDateTime.now(), tosVersion = "1.0", privacyAcceptedAt = LocalDateTime.now(),
    )

    @Nested
    inner class CreateUserEndpointTest {

        private fun validRequest(
            username: String = "betauser",
            email: String = "beta@example.com",
            password: String = "secret123",
            confirmPassword: String = "secret123",
        ) = AdminCreateUserRequest(username, password, confirmPassword, email)

        @Test
        fun `POST admin-users with admin token should return 200 with BETA_TESTER user`() {
            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(validRequest()))
            } When {
                post("/api/admin/users")
            } Then {
                statusCode(200)
                body("username", equalTo("betauser"))
                body("email", equalTo("beta@example.com"))
                body("subscriptionPlan", equalTo("BETA_TESTER"))
                body("id", notNullValue())
            }
        }

        @Test
        fun `POST admin-users without admin token should return 403`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(validRequest()))
            } When {
                post("/api/admin/users")
            } Then {
                statusCode(403)
            }
        }

        @Test
        fun `POST admin-users without token should return 401`() {
            Given {
                port(port)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(validRequest()))
            } When {
                post("/api/admin/users")
            } Then {
                statusCode(401)
            }
        }

        @Test
        fun `POST admin-users with mismatched passwords should return 400`() {
            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(validRequest(password = "abc123", confirmPassword = "different")))
            } When {
                post("/api/admin/users")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `POST admin-users with blank email should return 400`() {
            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(validRequest(email = "")))
            } When {
                post("/api/admin/users")
            } Then {
                statusCode(400)
            }
        }
    }

    @Nested
    inner class GetUsersEndpointTest {

        @Test
        fun `Get users as admin should return 200 with paginated users`() {
            registerUserUseCase.handle(consentCommand("user1", "user1@example.com"))
            registerUserUseCase.handle(consentCommand("user2", "user2@example.com"))
            registerUserUseCase.handle(consentCommand("user3", "user3@example.com"))

            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                queryParam("page", 0)
                queryParam("size", 10)
            } When {
                get("/api/admin/users")
            } Then {
                statusCode(200)
                body("pageNumber", equalTo(0))
                body("pageSize", equalTo(10))
                body("content[0].username", notNullValue())
                body("content[0].id", notNullValue())
                body("content[0].createdDate", notNullValue())
            }
        }

        @Test
        fun `Get users as admin with bearer token should return 200 with paginated users`() {
            registerUserUseCase.handle(consentCommand("user4", "user4@example.com"))

            Given {
                port(port)
                header("Authorization", "Bearer $adminToken")
                header("Content-Type", "application/json")
                queryParam("page", 0)
                queryParam("size", 10)
            } When {
                get("/api/admin/users")
            } Then {
                statusCode(200)
                body("pageNumber", equalTo(0))
                body("pageSize", equalTo(10))
                body("content[0].username", notNullValue())
            }
        }

        @Test
        fun `Get users with pagination should return correct page size`() {
            repeat(5) { i ->
                registerUserUseCase.handle(consentCommand("user$i", "user$i@example.com"))
            }

            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                queryParam("page", 0)
                queryParam("size", 3)
            } When {
                get("/api/admin/users")
            } Then {
                statusCode(200)
                body("content.size()", equalTo(3))
                body("pageNumber", equalTo(0))
                body("pageSize", equalTo(3))
            }
        }

        @Test
        fun `Get users on second page should return correct content`() {
            repeat(5) { i ->
                registerUserUseCase.handle(consentCommand("user$i", "user$i@example.com"))
            }

            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                queryParam("page", 1)
                queryParam("size", 3)
            } When {
                get("/api/admin/users")
            } Then {
                statusCode(200)
                body("content.size()", equalTo(3))
                body("pageNumber", equalTo(1))
                body("pageSize", equalTo(3))
            }
        }

        @Test
        fun `Get users without admin token should return 403`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                queryParam("page", 0)
                queryParam("size", 10)
            } When {
                get("/api/admin/users")
            } Then {
                statusCode(403)
            }
        }

        @Test
        fun `Get users without token should return 401`() {
            Given {
                port(port)
                header("Content-Type", "application/json")
                queryParam("page", 0)
                queryParam("size", 10)
            } When {
                get("/api/admin/users")
            } Then {
                statusCode(401)
            }
        }

        @Test
        fun `Get users with invalid token should return 401`() {
            Given {
                port(port)
                cookie("token", "invalid-token")
                header("Content-Type", "application/json")
                queryParam("page", 0)
                queryParam("size", 10)
            } When {
                get("/api/admin/users")
            } Then {
                statusCode(401)
            }
        }

        @Test
        fun `Get users should return users sorted by creation date descending`() {
            registerUserUseCase.handle(consentCommand("user1", "user1@example.com"))
            registerUserUseCase.handle(consentCommand("user2", "user2@example.com"))
            registerUserUseCase.handle(consentCommand("user3", "user3@example.com"))

            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                queryParam("page", 0)
                queryParam("size", 10)
            } When {
                get("/api/admin/users")
            } Then {
                statusCode(200)
                body("content[0].username", notNullValue())
                body("content[0].createdDate", notNullValue())
            }
        }

        @Test
        fun `Get users with page beyond available pages should return empty content`() {
            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                queryParam("page", 100)
                queryParam("size", 10)
            } When {
                get("/api/admin/users")
            } Then {
                statusCode(200)
                body("content.size()", equalTo(0))
                body("pageNumber", equalTo(100))
            }
        }

        @Test
        fun `Get users should return users with all required fields`() {
            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                queryParam("page", 0)
                queryParam("size", 10)
            } When {
                get("/api/admin/users")
            } Then {
                statusCode(200)
                body("content[0].id", notNullValue())
                body("content[0].username", notNullValue())
                body("content[0].createdDate", notNullValue())
            }
        }
    }
}

