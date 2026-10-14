package fr.sacane.jmanager.application.api

import fr.sacane.jmanager.application.api.setup.BookletStateTestAdapter
import fr.sacane.jmanager.application.api.setup.BookletTransaction
import fr.sacane.jmanager.application.api.setup.TransactionStateTestAdapter
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.transaction.Transaction
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
import java.time.LocalDate
import java.time.YearMonth

/** GET /api/transaction/search: every booklet of the signed-in user, month by month. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class TransactionSearchApiTest(
    @LocalServerPort private val port: Int,
    @Autowired private val bookletStateAdapter: BookletStateTestAdapter,
    @Autowired private val transactionStateTestAdapter: TransactionStateTestAdapter,
    @Autowired private val userRepositoryJpaAdapter: UserRepositoryJpaAdapter,
) : AuthenticatedUserTest() {

    private val thisMonth: YearMonth = YearMonth.now()

    @AfterEach
    fun clear() = bookletStateAdapter.clear()

    @Test
    fun `shouldFindMatchesAcrossBooklets_groupedByMonth`() {
        givenBooklet("Compte courant", spent("Péage A6", thisMonth.atDay(1)), spent("Courses", thisMonth.atDay(1)))
        givenBooklet("Livret A", spent("Peage retour", thisMonth.minusMonths(2).atDay(1)))

        Given {
            port(port)
            cookie("token", token)
            queryParam("q", "PEAGE")
        } When {
            get("/api/transaction/search")
        } Then {
            statusCode(200)
            body("months.month", equalTo(listOf(thisMonth.toString(), thisMonth.minusMonths(2).toString())))
            body("months[0].transactions[0].transaction.label", equalTo("Péage A6"))
            body("months[0].transactions[0].bookletLabel", equalTo("Compte courant"))
            body("months[1].transactions[0].bookletLabel", equalTo("Livret A"))
            body("regulars.size()", equalTo(0))
            body("from", equalTo(thisMonth.minusMonths(11).atDay(1).toString()))
            body("hasOlderTransactions", equalTo(false))
        }
    }

    // Same bound as a transaction label.
    @Test
    fun `shouldRefuseASearchLongerThanOneHundredCharacters`() {
        Given {
            port(port)
            cookie("token", token)
            queryParam("q", "a".repeat(101))
        } When {
            get("/api/transaction/search")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun `shouldRefuseAWindowOutsideTheAllowedRange`() {
        Given {
            port(port)
            cookie("token", token)
            queryParam("q", "loyer")
            queryParam("months", 121)
        } When {
            get("/api/transaction/search")
        } Then {
            statusCode(400)
            body("errorKey", equalTo("domain.transaction.search.invalid_window"))
        }
    }

    @Test
    fun `shouldRefuseTheBookletOfSomeoneElse`() {
        val stranger = userRepositoryJpaAdapter.register("stranger", "irrelevant-hash", emptySet(), email = "stranger@example.com")!!
        bookletStateAdapter.init(listOf(Booklet(label = "Leur compte", amount = Amount(100L), owner = stranger)))
        val theirs = bookletStateAdapter.get().first { it.label == "Leur compte" }.id

        Given {
            port(port)
            cookie("token", token)
            queryParam("q", "loyer")
            queryParam("bookletId", theirs.toString())
        } When {
            get("/api/transaction/search")
        } Then {
            statusCode(403)
        }
    }

    @Test
    fun `shouldRequireASignedInUser`() {
        Given {
            port(port)
            queryParam("q", "loyer")
        } When {
            get("/api/transaction/search")
        } Then {
            statusCode(401)
        }
    }

    private fun givenBooklet(label: String, vararg transactions: Transaction) {
        bookletStateAdapter.init(listOf(Booklet(label = label, amount = Amount(1000L), owner = user)))
        transactionStateTestAdapter.init(listOf(BookletTransaction(user!!.id, label, transactions.toList(), token)))
    }

    private fun spent(label: String, date: LocalDate) = Transaction(
        id = null,
        label = label,
        date = date,
        amount = Amount(10L),
        isIncome = false,
    )
}
