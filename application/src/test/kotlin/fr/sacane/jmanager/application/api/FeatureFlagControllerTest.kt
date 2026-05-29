package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.domain.port.input.user.CreateAdminIfNotExistsCommand
import fr.sacane.jmanager.domain.port.input.user.CreateAdminIfNotExistsUseCase
import fr.sacane.jmanager.domain.port.input.user.LoginCommand
import fr.sacane.jmanager.domain.port.input.user.LoginUseCase
import fr.sacane.jmanager.infrastructure.spi.repositories.FeatureFlagJpaRepository
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class FeatureFlagControllerTest(
    @LocalServerPort private val port: Int,
    @Autowired private val featureFlagJpaRepository: FeatureFlagJpaRepository,
    @Autowired private val createAdminIfNotExistsUseCase: CreateAdminIfNotExistsUseCase,
    @Autowired private val loginUseCase: LoginUseCase,
) : AuthenticatedUserTest() {

    private lateinit var adminToken: String

    @BeforeEach
    fun setupAdmin() {
        createAdminIfNotExistsUseCase.handle(CreateAdminIfNotExistsCommand("admin-flag", "admin123"))
        loginUseCase.handle(LoginCommand("admin-flag", "admin123")).onSuccess {
            adminToken = it.token
        }
    }

    @AfterEach
    fun clearFlags() {
        featureFlagJpaRepository.deleteAll()
    }

    @Nested
    inner class GetFeatureFlagsTest {

        @Test
        fun `GET feature-flags without authentication returns 200`() {
            Given {
                port(port)
            } When {
                get("/api/feature-flags")
            } Then {
                statusCode(200)
            }
        }

        @Test
        fun `GET feature-flags returns all known flag keys`() {
            Given {
                port(port)
            } When {
                get("/api/feature-flags")
            } Then {
                statusCode(200)
                body("size()", equalTo(FeatureKey.entries.size))
            }
        }

        @Test
        fun `GET feature-flags returns each flag with key and enabled fields`() {
            Given {
                port(port)
            } When {
                get("/api/feature-flags")
            } Then {
                statusCode(200)
                body("[0].key", notNullValue())
                body("[0].enabled", notNullValue())
            }
        }

        @Test
        fun `GET feature-flags returns disabled flags by default`() {
            Given {
                port(port)
            } When {
                get("/api/feature-flags")
            } Then {
                statusCode(200)
                body("find { it.key == 'EMAIL_VERIFICATION' }.enabled", equalTo(false))
            }
        }
    }

    @Nested
    inner class ToggleFeatureFlagTest {

        @Test
        fun `PATCH toggle by admin returns 200 with updated flag`() {
            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                body("""{"enabled": true}""")
            } When {
                patch("/api/admin/feature-flags/EMAIL_VERIFICATION")
            } Then {
                statusCode(200)
                body("key", equalTo("EMAIL_VERIFICATION"))
                body("enabled", equalTo(true))
            }
        }

        @Test
        fun `PATCH toggle is reflected in subsequent GET`() {
            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                body("""{"enabled": true}""")
            } When {
                patch("/api/admin/feature-flags/EMAIL_VERIFICATION")
            } Then {
                statusCode(200)
            }

            Given {
                port(port)
            } When {
                get("/api/feature-flags")
            } Then {
                statusCode(200)
                body("find { it.key == 'EMAIL_VERIFICATION' }.enabled", equalTo(true))
            }
        }

        @Test
        fun `PATCH toggle without admin token returns 403`() {
            Given {
                port(port)
                cookie("token", token)
                header("Content-Type", "application/json")
                body("""{"enabled": true}""")
            } When {
                patch("/api/admin/feature-flags/EMAIL_VERIFICATION")
            } Then {
                statusCode(403)
            }
        }

        @Test
        fun `PATCH toggle without any token returns 401`() {
            Given {
                port(port)
                header("Content-Type", "application/json")
                body("""{"enabled": true}""")
            } When {
                patch("/api/admin/feature-flags/EMAIL_VERIFICATION")
            } Then {
                statusCode(401)
            }
        }

        @Test
        fun `PATCH toggle with unknown flag key returns 400`() {
            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                body("""{"enabled": true}""")
            } When {
                patch("/api/admin/feature-flags/DOES_NOT_EXIST")
            } Then {
                statusCode(400)
            }
        }

        @Test
        fun `PATCH toggle can disable an enabled flag`() {
            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                body("""{"enabled": true}""")
            } When {
                patch("/api/admin/feature-flags/EMAIL_VERIFICATION")
            }

            Given {
                port(port)
                cookie("token", adminToken)
                header("Content-Type", "application/json")
                body("""{"enabled": false}""")
            } When {
                patch("/api/admin/feature-flags/EMAIL_VERIFICATION")
            } Then {
                statusCode(200)
                body("enabled", equalTo(false))
            }
        }
    }
}
