package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.application.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.infrastructure.spi.adapters.UserRepositoryJpaAdapter
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource

/**
 * The budget target of a booklet, read with the user settings and written one booklet at a time (UX-23).
 * Amounts travel as strings, like every amount of this API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class BudgetTargetApiTest(
    @LocalServerPort private val port: Int,
    @Autowired private val bookletStateAdapter: BookletStateTestAdapter,
    @Autowired private val userRepositoryJpaAdapter: UserRepositoryJpaAdapter,
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() = bookletStateAdapter.clear()

    @Test
    fun `shouldReturnTheTargetWithTheSettings_onceSet`() {
        val bookletId = givenBooklet()

        put(bookletId, """{"amount": "1500.5"}""").then().statusCode(204)

        settings().then()
            .statusCode(200)
            .body("bookletCycles[0].budgetTarget", equalTo("1500.50"))
    }

    @Test
    fun `shouldReturnNoTarget_onceRemoved`() {
        val bookletId = givenBooklet()
        put(bookletId, """{"amount": "1500"}""")

        Given {
            port(port)
            cookie("token", token)
        } When {
            delete("/api/user/settings/budget-targets/$bookletId")
        } Then {
            statusCode(204)
        }

        settings().then().body("bookletCycles[0].budgetTarget", nullValue())
    }

    @Test
    fun `shouldRefuseANegativeTarget`() {
        val bookletId = givenBooklet()

        put(bookletId, """{"amount": "-10"}""").then()
            .statusCode(400)
            .body("errorKey", equalTo("domain.user.settings.negative_budget_target"))
    }

    @Test
    fun `shouldRefuseTheBookletOfSomeoneElse`() {
        val stranger = userRepositoryJpaAdapter.register("stranger", "irrelevant-hash", emptySet(), email = "stranger@example.com")!!
        bookletStateAdapter.init(listOf(Booklet(label = "Leur compte", amount = Amount(100L), owner = stranger)))
        val theirs = bookletStateAdapter.get().first { it.label == "Leur compte" }.id.toString()

        put(theirs, """{"amount": "1500"}""").then().statusCode(403)
    }

    @Test
    fun `shouldRefuseABookletIdThatIsNotOne`() {
        put("not-an-id", """{"amount": "1500"}""").then().statusCode(400)
    }

    @Test
    fun `shouldRequireASignedInUser`() {
        Given {
            port(port)
            header("Content-Type", "application/json")
            body("""{"amount": "1500"}""")
        } When {
            put("/api/user/settings/budget-targets/${java.util.UUID.randomUUID()}")
        } Then {
            statusCode(401)
        }
    }

    private fun givenBooklet(): String {
        bookletStateAdapter.init(listOf(Booklet(label = "Compte courant", amount = Amount(1000L), owner = user)))
        return bookletStateAdapter.get().first { it.label == "Compte courant" }.id.toString()
    }

    private fun put(bookletId: String, json: String) = Given {
        port(port)
        cookie("token", token)
        header("Content-Type", "application/json")
        body(json)
    } When {
        put("/api/user/settings/budget-targets/$bookletId")
    }

    private fun settings() = Given {
        port(port)
        cookie("token", token)
    } When {
        get("/api/user/settings")
    }
}
