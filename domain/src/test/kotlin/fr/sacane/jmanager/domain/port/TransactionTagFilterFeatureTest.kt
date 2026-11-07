package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdBookletByTransaction
import fr.sacane.jmanager.domain.fake.IdUserBooklet
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fixture.BookletFixture
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.models.Tag
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

/** Filtering the period a booklet shows by tag: a tag brings its sub-tags along, a sub-tag stands alone. */
class TransactionTagFilterFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val bookletId: UUID = UUID.randomUUID()

    private val food = Tag.Personal("Alimentation", id = UUID.randomUUID())
    private val restaurant = Tag.Personal("Restaurant", id = UUID.randomUUID(), parentId = food.id)
    private val transport = Tag.Personal("Transport", id = UUID.randomUUID())

    @AfterEach
    fun afterEach() = factory.clearAll()

    @Test
    fun `shouldKeepTheTransactionsOfTheTag_andOfItsSubTags`() {
        val userId = givenTransactions(
            spent("Courses", day = 3, tag = food),
            spent("Pizzeria", day = 8, tag = restaurant),
            spent("Essence", day = 12, tag = transport),
            spent("Sans tag", day = 15),
        )

        val result = load(userId, tagId = food.id)

        assertEquals(listOf("Courses", "Pizzeria"), result.labels().sorted())
        assertEquals(2L, result.totalElements)
    }

    @Test
    fun `shouldKeepOnlyTheSubTag_whenTheFilterIsOne`() {
        val userId = givenTransactions(spent("Courses", day = 3, tag = food), spent("Pizzeria", day = 8, tag = restaurant))

        assertEquals(listOf("Pizzeria"), load(userId, tagId = restaurant.id).labels())
    }

    // The list is paginated: filtering only the displayed page would miss most matches.
    @Test
    fun `shouldFilterEveryPageOfThePeriod_notOnlyTheOneDisplayed`() {
        val unrelated = (1..12).map { spent("Divers $it", day = it, tag = transport) }
        val userId = givenTransactions(*(unrelated + spent("Restaurant", day = 28, tag = restaurant)).toTypedArray())

        val result = load(userId, tagId = food.id, pageSize = 5)

        assertEquals(listOf("Restaurant"), result.labels())
        assertEquals(1L, result.totalElements)
        assertEquals(1, result.totalPages)
    }

    @Test
    fun `shouldFilterPrevisionalTransactionsToo`() {
        val userId = givenTransactions(spent("Cantine", day = 5, tag = food, preview = true), spent("Essence", day = 8, tag = transport))

        assertEquals(listOf("Cantine"), load(userId, tagId = food.id).labels())
    }

    @Test
    fun `shouldCombineWithTheSearch`() {
        val userId = givenTransactions(
            spent("Courses Carrefour", day = 3, tag = food),
            spent("Courses marché", day = 8, tag = food),
            spent("Carrefour essence", day = 12, tag = transport),
        )

        assertEquals(listOf("Courses Carrefour"), load(userId, tagId = food.id, search = "carrefour").labels())
    }

    @Test
    fun `shouldNotFilter_whenNoTagIsGiven`() {
        val userId = givenTransactions(spent("Courses", day = 3, tag = food), spent("Sans tag", day = 8))

        assertEquals(2L, load(userId, tagId = null).totalElements)
    }

    // The tags to pick from describe the period, not the page shown nor the filters applied.
    @Test
    fun `shouldTellTheTagsUsedOverThePeriod_whateverThePageAndTheFilters`() {
        val userId = givenTransactions(
            spent("Courses", day = 3, tag = food),
            spent("Pizzeria", day = 8, tag = restaurant),
            spent("Essence", day = 12, tag = transport),
            spent("Sans tag", day = 15),
        )

        val result = load(userId, tagId = transport.id, search = "essence", pageSize = 1)

        assertEquals(setOf(food.id, restaurant.id, transport.id), result.periodTagIds)
    }

    // The balances describe the booklet over the period, not the filtered list.
    @Test
    fun `shouldKeepThePeriodBalances_whateverTheTag`() {
        val userId = givenTransactions(spent("Courses", day = 3, tag = food), spent("Essence", day = 8, tag = transport, preview = true))

        val all = load(userId)
        val filtered = load(userId, tagId = food.id)

        assertEquals(all.realSold, filtered.realSold)
        assertEquals(all.previsionalSold, filtered.previsionalSold)
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

    private fun spent(label: String, day: Int, tag: Tag? = null, preview: Boolean = false) = Transaction(
        id = UUID.randomUUID(),
        label = label,
        date = LocalDate.of(2025, Month.JANUARY, day),
        amount = 10.toAmount(),
        isIncome = false,
        isPreview = preview,
        tag = tag,
    )

    private fun load(
        userId: UserId,
        tagId: UUID? = null,
        search: String? = null,
        pageSize: Int = 50,
    ): BookletLoadingResult =
        factory.loadTransactionsForBookletForAMonthService.handle(
            LoadTransactionsForBookletForAMonthQuery(
                userId, bookletId, Month.JANUARY, 2025,
                startingMonth = Month.JANUARY, startingYear = 2025,
                pageSize = pageSize,
                search = search,
                tagId = tagId,
            ),
        ).mapNotNullOrFailure()!!

    private fun BookletLoadingResult.labels(): List<String> = orderedTransactions.map { it.label }
}
