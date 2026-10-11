package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdBookletByTransaction
import fr.sacane.jmanager.domain.fake.IdUserBooklet
import fr.sacane.jmanager.domain.fixture.BookletFixture
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.input.booklet.BookletLoadingResult
import fr.sacane.jmanager.domain.port.input.booklet.LoadTransactionsForBookletForAMonthQuery
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.util.UUID

/** Searching the period a booklet shows: one month, or the date range picked for it. */
class TransactionSearchFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val bookletId: UUID = UUID.randomUUID()

    @AfterEach
    fun afterEach() = factory.clearAll()

    @Test
    fun `shouldKeepOnlyTheTransactionsOfThePeriodWhoseLabelContainsTheFragment`() {
        val userId = givenTransactions(
            spent("Courses Carrefour", day = 3),
            spent("Essence", day = 8),
            spent("Carrefour drive", day = 21),
        )

        val result = load(userId, search = "carrefour")

        assertEquals(listOf("Courses Carrefour", "Carrefour drive"), result.labels().sorted().reversed())
        assertEquals(2L, result.totalElements)
    }

    @Test
    fun `shouldIgnoreCaseAndAccents`() {
        val userId = givenTransactions(spent("Péage autoroute", day = 3), spent("Essence", day = 8))

        assertEquals(listOf("Péage autoroute"), load(userId, search = "PEAGE").labels())
    }

    // The list is paginated: searching only the displayed page would miss most matches.
    @Test
    fun `shouldSearchEveryPageOfThePeriod_notOnlyTheOneDisplayed`() {
        val unrelated = (1..12).map { spent("Divers $it", day = it) }
        val userId = givenTransactions(*(unrelated + spent("Loyer", day = 28)).toTypedArray())

        val result = load(userId, search = "loyer", pageSize = 5)

        assertEquals(listOf("Loyer"), result.labels())
        assertEquals(1L, result.totalElements)
        assertEquals(1, result.totalPages)
    }

    @Test
    fun `shouldSearchPrevisionalTransactionsToo`() {
        val userId = givenTransactions(spent("Loyer", day = 5, preview = true), spent("Essence", day = 8))

        assertEquals(listOf("Loyer"), load(userId, search = "loyer").labels())
    }

    @Test
    fun `shouldNotLookOutsideTheDisplayedPeriod`() {
        val userId = givenTransactions(
            spent("Loyer", day = 5),
            spent("Essence", day = 9),
            spent("Loyer", day = 5, month = Month.FEBRUARY),
        )

        val result = load(userId, search = "loyer")

        assertEquals(1L, result.totalElements)
    }

    @Test
    fun `shouldSearchTheDateRangePickedForTheBooklet`() {
        val userId = givenTransactions(
            spent("Loyer", day = 5),
            spent("Essence", day = 9, month = Month.FEBRUARY),
            spent("Loyer", day = 5, month = Month.FEBRUARY),
            spent("Loyer", day = 5, month = Month.MARCH),
        )

        val result = load(
            userId,
            search = "loyer",
            startDate = LocalDate.of(2025, 1, 1),
            endDate = LocalDate.of(2025, 2, 28),
        )

        assertEquals(2L, result.totalElements)
    }

    @Test
    fun `shouldNotFilter_whenTheSearchIsBlank`() {
        val userId = givenTransactions(spent("Courses", day = 3), spent("Essence", day = 8))

        assertEquals(load(userId).labels(), load(userId, search = "   ").labels())
    }

    // The balances describe the booklet over the period, not the search result.
    @Test
    fun `shouldKeepThePeriodBalances_whateverTheSearch`() {
        val userId = givenTransactions(spent("Courses", day = 3), spent("Essence", day = 8, preview = true))

        val all = load(userId)
        val searched = load(userId, search = "courses")

        assertEquals(all.realSold, searched.realSold)
        assertEquals(all.previsionalSold, searched.previsionalSold)
    }

    @Test
    fun `shouldReturnAnEmptyPage_whenNothingMatches`() {
        val userId = givenTransactions(spent("Courses", day = 3))

        val result = load(userId, search = "introuvable")

        assertEquals(emptyList<String>(), result.labels())
        assertEquals(0L, result.totalElements)
    }

    private fun givenTransactions(vararg transactions: Transaction): UserId {
        val booklet = BookletFixture.aBooklet(id = bookletId, label = "Compte courant", amount = 1000.toAmount())
        val ctx = scenario.withUser().withBooklet(booklet)
        factory.fakeTransactionRepository().initWith(
            IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), transactions.toMutableList()),
        )
        factory.regularTransactionState.init(emptyList())
        return ctx.userId
    }

    private fun spent(label: String, day: Int, month: Month = Month.JANUARY, preview: Boolean = false) = Transaction(
        id = UUID.randomUUID(),
        label = label,
        date = LocalDate.of(2025, month, day),
        amount = 10.toAmount(),
        isIncome = false,
        isPreview = preview,
    )

    private fun load(
        userId: UserId,
        search: String? = null,
        pageSize: Int = 50,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
    ): BookletLoadingResult =
        factory.loadTransactionsForBookletForAMonthService.handle(
            LoadTransactionsForBookletForAMonthQuery(
                userId, bookletId, Month.JANUARY, 2025,
                startingMonth = Month.JANUARY, startingYear = 2025,
                startDate = startDate, endDate = endDate,
                pageSize = pageSize,
                search = search,
            ),
        ).mapNotNullOrFailure()!!

    private fun BookletLoadingResult.labels(): List<String> = orderedTransactions.map { it.label }
}
