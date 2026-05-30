package fr.sacane.jmanager.application.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.input.user.LoginCommand
import fr.sacane.jmanager.domain.port.input.user.LoginUseCase
import fr.sacane.jmanager.domain.port.output.Hasher
import fr.sacane.jmanager.application.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.application.api.session.BookletMonthlyCycleUpdateDTO
import fr.sacane.jmanager.application.api.session.RecordConsentDTO
import fr.sacane.jmanager.application.api.session.UserPasswordDTO
import fr.sacane.jmanager.application.api.session.UserSettingsUpdateDTO
import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.infrastructure.spi.adapters.UserRepositoryJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.entity.FeatureFlagEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.FeatureFlagJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.util.UUID


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class SessionControllerTest(
    @LocalServerPort private val port: Int,
    @Autowired val objectMapper: ObjectMapper,
    @Autowired val userRepository: UserPostgresRepository,
    @Autowired private val BookletStateTestAdapter: BookletStateTestAdapter,
    @Autowired private val loginUseCase: LoginUseCase,
    @Autowired private val userRepositoryJpaAdapter: UserRepositoryJpaAdapter,
    @Autowired private val hasher: Hasher,
    @Autowired private val featureFlagJpaRepository: FeatureFlagJpaRepository,
): AuthenticatedUserTest() {

    @AfterEach
    fun clear() {
        BookletStateTestAdapter.clear()
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

        @Test
        fun `Refresh a connected user with cookie must return 200 and token`() {
            Given {
                port(port)
                cookie("refresh_token", refreshToken)
                header("Content-Type", "application/json")
            } When {
                post("/api/user/auth/refresh/${user!!.id.value}")
            } Then {
                statusCode(200)
                body("token", notNullValue())
            }
        }

        @Test
        fun `Refresh a connected user with bearer must return 200 and token`() {
            Given {
                port(port)
                header("Authorization", "Bearer $refreshToken")
                header("Content-Type", "application/json")
            } When {
                post("/api/user/auth/refresh/${user!!.id.value}")
            } Then {
                statusCode(200)
                body("token", notNullValue())
            }
        }

        @Test
        fun `Refresh a connected user with X-Refresh-Token header must return 200 and token`() {
            Given {
                port(port)
                header("X-Refresh-Token", refreshToken)
                header("Content-Type", "application/json")
            } When {
                post("/api/user/auth/refresh/${user!!.id.value}")
            } Then {
                statusCode(200)
                body("token", notNullValue())
            }
        }

        @Test
        fun `Refresh with another user id must return 401`() {
            Given {
                port(port)
                cookie("refresh_token", refreshToken)
                header("Content-Type", "application/json")
            } When {
                post("/api/user/auth/refresh/${UUID.randomUUID()}")
            } Then {
                statusCode(401)
            }
        }

        @Test
        fun `Refresh a connected user without user id must return 200 and token`() {
            Given {
                port(port)
                cookie("refresh_token", refreshToken)
                header("Content-Type", "application/json")
            } When {
                post("/api/user/auth/refresh")
            } Then {
                statusCode(200)
                body("token", notNullValue())
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

    @Nested
    inner class DeleteAccountEndpointTest {
        @Test
        fun `Delete own account with valid token must return 204`() {
            val userId = user!!.id.value!!

            Given {
                port(port)
                cookie("token", token)
            } When {
                delete("/api/user/me")
            } Then {
                statusCode(204)
            }

            assertNull(userRepository.findById(userId).orElse(null))
        }

        @Test
        fun `Delete account without token must return 401`() {
            Given {
                port(port)
            } When {
                delete("/api/user/me")
            } Then {
                statusCode(401)
            }
        }
    }

    @Nested
    inner class UserSettingsEndpointTest {
        @Test
        fun `Get user settings with bearer token must return 200`() {
            Given {
                port(port)
                header("Authorization", "Bearer $token")
                header("Content-Type", "application/json")
            } When {
                get("/api/user/settings")
            } Then {
                statusCode(200)
                body("projectionWindowDays", equalTo(15))
            }
        }

        @Test
        fun `Get user settings for authenticated user must return 200`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/user/settings")
            } Then {
                statusCode(200)
                body("projectionWindowDays", equalTo(15))
            }
        }

        @Test
        fun `Update user settings with valid projection must return 200`() {
            val body = UserSettingsUpdateDTO(
                projectionWindowDays = 30,
                bookletCycles = emptyList(),
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                put("/api/user/settings")
            } Then {
                statusCode(200)
                body("projectionWindowDays", equalTo(30))
            }
        }

        @Test
        fun `Update user settings with invalid projection must return 400`() {
            val body = UserSettingsUpdateDTO(
                projectionWindowDays = 6,
                bookletCycles = emptyList(),
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                put("/api/user/settings")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `Update user settings with invalid monthly period end day must return 400`() {
            BookletStateTestAdapter.init(listOf(Booklet(1000.toAmount(), "Compte principal", owner = user)))
            val bookletId = BookletStateTestAdapter.get().first().id!!.toString()

            val body = UserSettingsUpdateDTO(
                projectionWindowDays = 15,
                bookletCycles = listOf(
                    BookletMonthlyCycleUpdateDTO(
                        bookletId = bookletId,
                        monthlyPeriodStartDay = 20,
                        monthlyPeriodEndDay = 32,
                    ),
                ),
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                put("/api/user/settings")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `Update user settings with invalid booklet id must return 400`() {
            val body = UserSettingsUpdateDTO(
                projectionWindowDays = 15,
                bookletCycles = listOf(BookletMonthlyCycleUpdateDTO(bookletId = "not-a-uuid", monthlyPeriodStartDay = 20)),
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                put("/api/user/settings")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `Update user settings with non owned booklet must return 403`() {
            val body = UserSettingsUpdateDTO(
                projectionWindowDays = 15,
                bookletCycles = listOf(
                    BookletMonthlyCycleUpdateDTO(
                        bookletId = UUID.randomUUID().toString(),
                        monthlyPeriodStartDay = 20,
                    )
                ),
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                put("/api/user/settings")
            } Then {
                statusCode(403)
            }
        }

        @Test
        fun `Update user settings without cycles for owned booklets must return 400`() {
            BookletStateTestAdapter.init(listOf(Booklet(1000.toAmount(), "Compte principal", owner = user)))

            val body = UserSettingsUpdateDTO(
                projectionWindowDays = 20,
                bookletCycles = emptyList(),
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(body))
            } When {
                put("/api/user/settings")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `Update user settings with owned booklet must persist and be readable`() {
            BookletStateTestAdapter.init(listOf(Booklet(1000.toAmount(), "Compte principal", owner = user)))
            val bookletId = BookletStateTestAdapter.get().first().id!!.toString()

            val updateBody = UserSettingsUpdateDTO(
                projectionWindowDays = 30,
                bookletCycles = listOf(
                    BookletMonthlyCycleUpdateDTO(
                        bookletId = bookletId,
                        monthlyPeriodStartDay = 28,
                        monthlyPeriodEndDay = 27,
                    )
                ),
            )

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(updateBody))
            } When {
                put("/api/user/settings")
            } Then {
                statusCode(200)
                body("projectionWindowDays", equalTo(30))
                body("bookletCycles.size()", equalTo(1))
                body("bookletCycles[0].bookletId", equalTo(bookletId))
                body("bookletCycles[0].monthlyPeriodStartDay", equalTo(28))
                body("bookletCycles[0].monthlyPeriodEndDay", equalTo(27))
            }

            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
            } When {
                get("/api/user/settings")
            } Then {
                statusCode(200)
                body("projectionWindowDays", equalTo(30))
                body("bookletCycles.size()", equalTo(1))
                body("bookletCycles[0].bookletId", equalTo(bookletId))
                body("bookletCycles[0].monthlyPeriodStartDay", equalTo(28))
                body("bookletCycles[0].monthlyPeriodEndDay", equalTo(27))
            }
        }
    }
    @Nested
    inner class CreateUserEndpointTest {

        private val validPayload = """
            {
              "username": "newuser",
              "password": "pass123",
              "confirmPassword": "pass123",
              "email": "newuser@example.com",
              "tosAccepted": true,
              "tosVersion": "1.0",
              "privacyAccepted": true
            }
        """.trimIndent()

        @Test
        fun `POST create returns 200 when USER_REGISTRATION flag is enabled`() {
            Given {
                port(port)
                header("Content-Type", "application/json")
                body(validPayload)
            } When {
                post("/api/user/create")
            } Then {
                statusCode(200)
            }
        }

        @Test
        fun `POST create returns 404 when USER_REGISTRATION flag is disabled`() {
            featureFlagJpaRepository.save(FeatureFlagEntity(key = FeatureKey.USER_REGISTRATION.name, enabled = false))

            Given {
                port(port)
                header("Content-Type", "application/json")
                body(validPayload)
            } When {
                post("/api/user/create")
            } Then {
                statusCode(404)
            }
        }

        @Test
        fun `POST create with mismatched passwords returns 401 regardless of flag`() {
            val mismatchPayload = """
                {
                  "username": "newuser2",
                  "password": "pass123",
                  "confirmPassword": "other456",
                  "email": "newuser2@example.com",
                  "tosAccepted": true,
                  "tosVersion": "1.0",
                  "privacyAccepted": true
                }
            """.trimIndent()

            Given {
                port(port)
                header("Content-Type", "application/json")
                body(mismatchPayload)
            } When {
                post("/api/user/create")
            } Then {
                statusCode(401)
            }
        }
    }

    @Nested
    inner class ConsentEndpointTest {

        @Test
        fun `POST consent with valid data must return 204`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(RecordConsentDTO(tosAccepted = true, tosVersion = "1.0", privacyAccepted = true)))
            } When {
                post("/api/user/consent")
            } Then {
                statusCode(204)
            }
        }

        @Test
        fun `POST consent without tosAccepted must return 400`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(RecordConsentDTO(tosAccepted = false, tosVersion = "1.0", privacyAccepted = true)))
            } When {
                post("/api/user/consent")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `POST consent without authentication must return 401`() {
            Given {
                port(port)
                header("Content-Type", "application/json")
                body(objectMapper.writeValueAsString(RecordConsentDTO(tosAccepted = true, tosVersion = "1.0", privacyAccepted = true)))
            } When {
                post("/api/user/consent")
            } Then {
                statusCode(401)
            }
        }

        @Test
        fun `GET me for consented user must return consentRequired false`() {
            Given {
                port(port)
                cookie("token", token)
            } When {
                get("/api/user/me")
            } Then {
                statusCode(200)
                body("consentRequired", equalTo(false))
            }
        }

        @Test
        fun `GET me for non-consented user must return consentRequired true`() {
            // Simulates an admin-created account — bypasses domain validation, no consent stored.
            // Password must be pre-hashed since UserRepositoryJpaAdapter stores it as-is.
            userRepositoryJpaAdapter.register("no-consent-user", hasher.hash("pwd"), emptySet())
            var noConsentToken: String? = null
            loginUseCase.handle(LoginCommand("no-consent-user", "pwd"))
                .onSuccess { noConsentToken = it.token }

            Given {
                port(port)
                cookie("token", noConsentToken!!)
            } When {
                get("/api/user/me")
            } Then {
                statusCode(200)
                body("consentRequired", equalTo(true))
            }
        }

        @Test
        fun `GET me without authentication must return 401`() {
            Given {
                port(port)
            } When {
                get("/api/user/me")
            } Then {
                statusCode(401)
            }
        }
    }
}
