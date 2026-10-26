package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fake.UserBookletScenario
import fr.sacane.jmanager.domain.fixture.BookletFixture
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.TransactionSearchResult
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import fr.sacane.jmanager.domain.port.input.transaction.SearchTransactionsQuery
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

// The fake clock is 31 May 2026: the default window runs from June 2025 to May 2026.
private val RENT = RegularTransactionId("rent")

/** Searching every booklet of an account at once, month by month, regular transactions folded. */
class GlobalTransactionSearchFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val current = BookletFixture.aBooklet(id = UUID.randomUUID(), label = "Compte courant", amount = 1000.toAmount())
    private val savings = BookletFixture.aBooklet(id = UUID.randomUUID(), label = "Livret A", amount = 1000.toAmount())

    @AfterEach
    fun afterEach() = factory.clearAll()

    @Test
    fun `shouldFindMatchesInEveryBookletGroupedByMonthNewestFirst`() {
        val userId = givenBooklets(
            current to listOf(spent("Courses Carrefour", 2026, 5, 3), spent("Essence", 2026, 5, 9)),
            savings to listOf(spent("Carrefour drive", 2026, 3, 21)),
        )

        val result = search(userId, "carrefour")

        assertEquals(listOf(YearMonth.of(2026, 5), YearMonth.of(2026, 3)), result.months.map { it.month })
        assertEquals(listOf("Courses Carrefour" to "Compte courant"), result.months[0].transactions.map { it.transaction.label to it.bookletLabel })
        assertEquals(listOf("Carrefour drive" to "Livret A"), result.months[1].transactions.map { it.transaction.label to it.bookletLabel })
    }

    @Test
    fun `shouldIgnoreCaseAndAccents`() {
        val userId = givenBooklets(current to listOf(spent("Péage autoroute", 2026, 4, 2)))

        assertEquals(1, search(userId, "PEAGE").months.single().transactions.size)
    }

    @Test
    fun `shouldOrderTheTransactionsOfAMonthNewestFirst`() {
        val userId = givenBooklets(current to listOf(spent("Loyer parking", 2026, 5, 3), spent("Loyer garage", 2026, 5, 20)))

        assertEquals(listOf("Loyer garage", "Loyer parking"), search(userId, "loyer").months.single().transactions.map { it.transaction.label })
    }

    // Two years of a monthly rent would otherwise be 24 lines.
    @Test
    fun `shouldFoldTheOccurrencesOfARegularTransactionIntoOneLine`() {
        val occurrences = (1..5).map { month -> spent("Loyer", 2026, month, 5, regular = RENT) }
        val userId = givenBooklets(current to occurrences + spent("Loyer parking", 2026, 5, 3))

        val result = search(userId, "loyer")

        val rent = result.regulars.single()
        assertEquals("Loyer", rent.label)
        assertEquals("Compte courant", rent.bookletLabel)
        assertEquals(5, rent.occurrences.size)
        assertEquals(LocalDate.of(2026, 1, 5), rent.firstDate)
        assertEquals(LocalDate.of(2026, 5, 5), rent.lastDate)
        assertEquals(LocalDate.of(2026, 5, 5), rent.occurrences.first().transaction.date)
        assertEquals(listOf("Loyer parking"), result.months.flatMap { it.transactions }.map { it.transaction.label })
    }

    @Test
    fun `shouldKeepOneLinePerBooklet_whenARegularTransactionFeedsSeveralBooklets`() {
        val userId = givenBooklets(
            current to listOf(spent("Épargne", 2026, 5, 1, regular = RENT)),
            savings to listOf(spent("Épargne", 2026, 5, 1, regular = RENT)),
        )

        assertEquals(setOf("Compte courant", "Livret A"), search(userId, "epargne").regulars.map { it.bookletLabel }.toSet())
    }

    // Deleted from its month in the booklet view: it must not come back through the search.
    @Test
    fun `shouldHidePrevisionalOccurrencesExcludedFromTheirMonth`() {
        val userId = givenBooklets(
            current to listOf(
                spent("Loyer", 2026, 4, 5, regular = RENT, preview = true),
                spent("Loyer", 2026, 5, 5, regular = RENT, preview = true),
            ),
        )
        factory.trackerRepository().upsertTracker(
            RegularTransactionTracker(
                regularTransactionId = RENT,
                bookletId = current.id!!,
                lastGeneratedDate = LocalDate.of(2026, 5, 5),
                excludedMonths = setOf(YearMonth.of(2026, 5)),
            ),
        )

        assertEquals(listOf(LocalDate.of(2026, 4, 5)), search(userId, "loyer").regulars.single().occurrences.map { it.transaction.date })
    }

    @Test
    fun `shouldSearchTheLastTwelveMonthsByDefault_andFurtherOnRequest`() {
        val userId = givenBooklets(current to listOf(spent("Loyer parking", 2026, 5, 3), spent("Loyer parking", 2025, 5, 3)))

        val lastYear = search(userId, "loyer")
        val twoYears = search(userId, "loyer", monthsBack = 24)

        assertEquals(LocalDate.of(2025, 6, 1), lastYear.from)
        assertEquals(LocalDate.of(2026, 5, 31), lastYear.to)
        assertEquals(1, lastYear.months.size)
        assertEquals(2, twoYears.months.size)
    }

    // Whether "Chercher plus loin" can find anything: any older transaction, matching or not.
    @Test
    fun `shouldTellWhetherOlderTransactionsExist`() {
        val withHistory = givenBooklets(current to listOf(spent("Loyer parking", 2026, 5, 3), spent("Essence", 2024, 1, 3)))

        assertTrue(search(withHistory, "loyer").hasOlderTransactions)
        assertFalse(search(withHistory, "loyer", monthsBack = 36).hasOlderTransactions)
    }

    @Test
    fun `shouldSearchOneBooklet_whenAsked`() {
        val userId = givenBooklets(
            current to listOf(spent("Loyer parking", 2026, 5, 3)),
            savings to listOf(spent("Loyer garage", 2026, 5, 4)),
        )

        val result = search(userId, "loyer", bookletId = savings.id)

        assertEquals(listOf("Livret A"), result.months.flatMap { it.transactions }.map { it.bookletLabel })
    }

    @Test
    fun `shouldRefuse_whenTheBookletBelongsToSomeoneElse`() {
        val userId = givenBooklets(current to listOf(spent("Loyer", 2026, 5, 3)))
        val stranger = scenario.withUser(UserFixture.aUserWithPassword()).withBooklet(savings)
        UserBookletScenario(factory, stranger.user, savings).withTransactions(spent("Loyer", 2026, 5, 4))

        factory.searchTransactionsService.handle(SearchTransactionsQuery(userId, "loyer", bookletId = savings.id))
            .assertFailure(ResultState.FORBIDDEN)
    }

    @Test
    fun `shouldNeverSearchTheBookletsOfOtherUsers`() {
        val userId = givenBooklets(current to listOf(spent("Essence", 2026, 5, 3)))
        val stranger = scenario.withUser(UserFixture.aUserWithPassword()).withBooklet(savings)
        UserBookletScenario(factory, stranger.user, savings).withTransactions(spent("Loyer", 2026, 5, 4))

        assertTrue(search(userId, "loyer").months.isEmpty())
    }

    // An empty search over a whole year would list everything: it lists nothing.
    @Test
    fun `shouldFindNothing_whenTheSearchIsBlank`() {
        val userId = givenBooklets(current to listOf(spent("Loyer", 2026, 5, 3)))

        val result = search(userId, "   ")

        assertTrue(result.months.isEmpty() && result.regulars.isEmpty())
    }

    @Test
    fun `shouldRefuseAWindowOutsideOneToOneHundredAndTwentyMonths`() {
        val userId = givenBooklets(current to listOf(spent("Loyer", 2026, 5, 3)))

        factory.searchTransactionsService.handle(SearchTransactionsQuery(userId, "loyer", monthsBack = 0)).assertFailure(ResultState.INVALID)
        factory.searchTransactionsService.handle(SearchTransactionsQuery(userId, "loyer", monthsBack = 121)).assertFailure(ResultState.INVALID)
    }

    private fun givenBooklets(vararg booklets: Pair<Booklet, List<Transaction>>): UserId {
        val user = scenario.withUser(UserFixture.aUserWithPassword()).withBooklets(*booklets.map { it.first }.toTypedArray())
        booklets.forEach { (booklet, transactions) ->
            UserBookletScenario(factory, user.user, booklet).withTransactions(*transactions.toTypedArray())
        }
        return user.userId
    }

    private fun spent(
        label: String,
        year: Int,
        month: Int,
        day: Int,
        regular: RegularTransactionId? = null,
        preview: Boolean = false,
    ) = Transaction(
        id = UUID.randomUUID(),
        label = label,
        date = LocalDate.of(year, month, day),
        amount = 10.toAmount(),
        isIncome = false,
        isPreview = preview,
        regularTransactionId = regular,
    )

    private fun search(userId: UserId, text: String, monthsBack: Int = 12, bookletId: UUID? = null): TransactionSearchResult =
        factory.searchTransactionsService.handle(SearchTransactionsQuery(userId, text, monthsBack, bookletId)).mapNotNullOrFailure()!!
}
