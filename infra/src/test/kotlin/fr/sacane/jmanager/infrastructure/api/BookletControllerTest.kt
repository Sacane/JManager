package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.infrastructure.api.setup.AccountFakeTestAdapter
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class BookletControllerTest(
    @LocalServerPort val port: Int,
    @Autowired val accountFakeTestAdapter: AccountFakeTestAdapter
) {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        }
    }

    @AfterEach
    fun tearDown() {
        accountFakeTestAdapter.clear()
    }

    @Test
    fun test() {
        Given {
            port(port)
            header("Content-Type", "application/json")
        }
    }
}