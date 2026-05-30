package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.application.AbstractIntegrationTest
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort

class ActuatorInfoTest : AbstractIntegrationTest() {

    @LocalServerPort
    private val port: Int = 0

    @Test
    fun `GET actuator-info without authentication returns 200`() {
        Given {
            port(port)
        } When {
            get("/actuator/info")
        } Then {
            statusCode(200)
        }
    }

    @Test
    fun `GET actuator-info returns build version`() {
        Given {
            port(port)
        } When {
            get("/actuator/info")
        } Then {
            statusCode(200)
            body("build.version", notNullValue())
        }
    }
}
