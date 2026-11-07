package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.application.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.application.api.setup.BookletTransaction
import fr.sacane.jmanager.application.api.setup.TagStateTestAdapter
import fr.sacane.jmanager.application.api.setup.TransactionStateTestAdapter
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.Transaction
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

/** The transactions of a booklet's period filtered by tag on the server, every page of it, and the tags to pick from. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class TransactionTagFilterApiTest(
    @LocalServerPort private val port: Int,
    @Autowired private val bookletStateAdapter: BookletStateTestAdapter,
    @Autowired private val transactionStateTestAdapter: TransactionStateTestAdapter,
    @Autowired private val tagStateTestAdapter: TagStateTestAdapter,
) : AuthenticatedUserTest() {

    private val today: LocalDate = LocalDate.now()

    @AfterEach
    fun clear() = bookletStateAdapter.clear()

    @Test
    fun `shouldKeepTheTransactionsOfTheTag_onEveryPage`() {
        val (shopping, transport) = twoDefaultTags()
        val bookletId = givenTransactions(
            "Essence" to transport, "Péage" to transport, "Train" to transport, "Courses" to shopping,
        )

        transactions(bookletId, "tagId" to shopping.id.toString(), "size" to "2").then()
            .statusCode(200)
            .body("transactions.label", equalTo(listOf("Courses")))
            .body("totalElements", equalTo(1))
    }

    @Test
    fun `shouldTellTheTagsUsedOverThePeriod_whateverThePageAndTheFilter`() {
        val (shopping, transport) = twoDefaultTags()
        val bookletId = givenTransactions("Essence" to transport, "Courses" to shopping)

        transactions(bookletId, "tagId" to transport.id.toString(), "size" to "1").then()
            .statusCode(200)
            .body("tagIds", containsInAnyOrder(shopping.id.toString(), transport.id.toString()))
    }

    @Test
    fun `shouldRefuseATagIdentifierThatIsNotOne`() {
        val (shopping, _) = twoDefaultTags()
        val bookletId = givenTransactions("Courses" to shopping)

        transactions(bookletId, "tagId" to "not-a-tag").then().statusCode(400)
    }

    private fun twoDefaultTags(): Pair<Tag, Tag> {
        val defaults = tagStateTestAdapter.get().filterIsInstance<Tag.Default>().filter { it.label != "Aucune" }
        return defaults[0] to defaults[1]
    }

    private fun givenTransactions(vararg labelled: Pair<String, Tag>): String {
        val booklet = Booklet(amount = Amount.fromString("1000.00"), label = "Compte courant", owner = user)
        bookletStateAdapter.init(listOf(booklet))
        transactionStateTestAdapter.init(
            listOf(
                BookletTransaction(
                    user!!.id,
                    booklet.label,
                    transactions = labelled.mapIndexed { index, (label, tag) ->
                        Transaction(
                            id = null, label = label, amount = Amount.fromString("10.00"),
                            date = today.withDayOfMonth(1).plusDays(index.toLong()), isPreview = false, isIncome = false,
                            tag = tag,
                        )
                    },
                    token = token,
                )
            )
        )
        return bookletStateAdapter.get().first().id.toString()
    }

    private fun transactions(bookletId: String, vararg params: Pair<String, String>) = Given {
        port(port)
        cookie("token", token)
        queryParam("month", today.monthValue)
        queryParam("year", today.year)
        queryParams(params.toMap())
    } When {
        get("/api/booklet/$bookletId/transactions")
    }
}
