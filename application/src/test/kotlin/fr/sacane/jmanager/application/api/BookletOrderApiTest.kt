package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.application.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.infrastructure.spi.adapters.UserRepositoryJpaAdapter
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.util.UUID

/** The order the user gives their booklets, kept with the account and followed by every list of them. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class BookletOrderApiTest(
    @LocalServerPort private val port: Int,
    @Autowired private val bookletStateAdapter: BookletStateTestAdapter,
    @Autowired private val userRepositoryJpaAdapter: UserRepositoryJpaAdapter,
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear() = bookletStateAdapter.clear()

    @Test
    fun `shouldListTheBookletsInTheChosenOrder`() {
        val ids = givenBooklets("Compte courant", "Livret A", "Compte joint")

        reorder(ids.getValue("Compte joint"), ids.getValue("Compte courant"), ids.getValue("Livret A")).then().statusCode(204)

        Given {
            port(port)
            cookie("token", token)
        } When {
            get("/api/booklet")
        } Then {
            statusCode(200)
            body("label", equalTo(listOf("Compte joint", "Compte courant", "Livret A")))
        }
    }

    @Test
    fun `shouldShowTheSameOrderInTheSettings`() {
        val ids = givenBooklets("Compte courant", "Livret A")

        reorder(ids.getValue("Livret A"), ids.getValue("Compte courant"))

        Given {
            port(port)
            cookie("token", token)
        } When {
            get("/api/user/settings")
        } Then {
            statusCode(200)
            body("bookletCycles.label", equalTo(listOf("Livret A", "Compte courant")))
        }
    }

    @Test
    fun `shouldRefuseABookletGivenTwice`() {
        val ids = givenBooklets("Compte courant", "Livret A")

        reorder(ids.getValue("Livret A"), ids.getValue("Livret A")).then()
            .statusCode(400)
            .body("errorKey", equalTo("domain.booklet.order.duplicate"))
    }

    @Test
    fun `shouldRefuseAnEmptyOrder`() {
        put("""{"bookletIds": []}""").then().statusCode(400)
    }

    @Test
    fun `shouldRefuseTheBookletOfSomeoneElse`() {
        val ids = givenBooklets("Compte courant")
        val stranger = userRepositoryJpaAdapter.register("stranger", "irrelevant-hash", emptySet(), email = "stranger@example.com")!!
        bookletStateAdapter.init(listOf(Booklet(label = "Leur compte", amount = Amount(100L), owner = stranger)))
        val theirs = bookletStateAdapter.get().first { it.label == "Leur compte" }.id.toString()

        reorder(ids.getValue("Compte courant"), theirs).then().statusCode(403)
    }

    @Test
    fun `shouldRefuseAnIdentifierThatIsNotOne`() {
        put("""{"bookletIds": ["not-an-id"]}""").then().statusCode(400)
    }

    @Test
    fun `shouldRefuseMoreIdentifiersThanAnAccountCouldHold`() {
        val tooMany = (1..21).joinToString(",") { "\"${UUID.randomUUID()}\"" }

        put("""{"bookletIds": [$tooMany]}""").then().statusCode(400)
    }

    @Test
    fun `shouldRequireASignedInUser`() {
        Given {
            port(port)
            header("Content-Type", "application/json")
            body("""{"bookletIds": ["${UUID.randomUUID()}"]}""")
        } When {
            put("/api/booklet/order")
        } Then {
            statusCode(401)
        }
    }

    private fun givenBooklets(vararg labels: String): Map<String, String> {
        bookletStateAdapter.init(labels.map { Booklet(label = it, amount = Amount(1000L), owner = user) })
        return bookletStateAdapter.get().filter { it.label in labels }.associate { it.label to it.id.toString() }
    }

    private fun reorder(vararg bookletIds: String) =
        put("""{"bookletIds": [${bookletIds.joinToString(",") { "\"$it\"" }}]}""")

    private fun put(json: String) = Given {
        port(port)
        cookie("token", token)
        header("Content-Type", "application/json")
        body(json)
    } When {
        put("/api/booklet/order")
    }
}
