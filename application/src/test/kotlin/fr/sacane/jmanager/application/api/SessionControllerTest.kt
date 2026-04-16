package fr.sacane.jmanager.application.api

import com.fasterxml.jackson.databind.ObjectMapper
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.application.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.application.api.session.BookletMonthlyCycleUpdateDTO
import fr.sacane.jmanager.application.api.session.UserPasswordDTO
import fr.sacane.jmanager.application.api.session.UserSettingsUpdateDTO
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.AfterEach
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
