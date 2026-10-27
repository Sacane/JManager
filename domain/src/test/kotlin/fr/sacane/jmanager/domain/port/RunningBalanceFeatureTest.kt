package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdBookletByTransaction
import fr.sacane.jmanager.domain.fake.IdUserBooklet
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fixture.BookletFixture
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.TransactionSortDirection
import fr.sacane.jmanager.domain.models.transaction.TransactionSortField
import fr.sacane.jmanager.domain.port.input.booklet.BookletLoadingResult
import fr.sacane.jmanager.domain.port.input.booklet.LoadTransactionsForBookletForAMonthQuery
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.UUID

/** Each confirmed transaction of a booklet shows the balance it leaves, as on a bank statement (UX-25). */
class RunningBalanceFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val bookletId: UUID = UUID.randomUUID()

    @AfterEach
    fun afterEach() = factory.clearAll()

    @Test
    fun `shouldShowTheBalanceLeftByEachConfirmedTransaction_fromTheBalanceAtThePeriodStart`() {
        val salary = income("Salaire", 2000, Month.FEBRUARY, 3)
        val groceries = spent("Courses", 60, Month.FEBRUARY, 8)
        val userId = givenBooklet(spent("Loyer janvier", 850, Month.JANUARY, 5), salary, groceries)

        val result = load(userId)

        // 1 000 initial, -850 in January: the period opens at 150.
        assertEquals(2150.toAmount(), result.balanceAfter[salary.id])
        assertEquals(2090.toAmount(), result.balanceAfter[groceries.id])
    }

    @Test
    fun `shouldLeavePrevisionalTransactionsOutOfTheBalance`() {
        val rentPreview = spent("Loyer", 850, Month.FEBRUARY, 5, preview = true)
        val groceries = spent("Courses", 60, Month.FEBRUARY, 8)
        val userId = givenBooklet(spent("Ancienne prévision", 500, Month.JANUARY, 20, preview = true), rentPreview, groceries)

        val result = load(userId)

        assertFalse(result.balanceAfter.containsKey(rentPreview.id))
        assertEquals(940.toAmount(), result.balanceAfter[groceries.id])
    }

    // Two transactions of the same day follow the order they were entered in.
    @Test
    fun `shouldOrderTheTransactionsOfADayByTheirEntry`() {
        val first = spent("Boulangerie", 5, Month.FEBRUARY, 8, enteredAt = LocalDateTime.of(2025, 2, 8, 9, 0))
        val second = spent("Pharmacie", 20, Month.FEBRUARY, 8, enteredAt = LocalDateTime.of(2025, 2, 8, 18, 0))
        val userId = givenBooklet(second, first)

        val result = load(userId)

        assertEquals(995.toAmount(), result.balanceAfter[first.id])
        assertEquals(975.toAmount(), result.balanceAfter[second.id])
    }

    // Every line carries its own balance: hiding or reordering lines changes none of them.
    @Test
    fun `shouldGiveEachLineTheSameBalance_whateverThePageTheSortOrTheSearch`() {
        val transactions = (1..12).map { spent("Dépense $it", 10, Month.FEBRUARY, it) }
        val userId = givenBooklet(*transactions.toTypedArray())
        val last = transactions.last()

        val paged = load(userId, pageSize = 5, pageNumber = 2)
        val byLabel = load(userId, sortField = TransactionSortField.LABEL)
        val searched = load(userId, search = "Dépense 12")

        assertEquals(880.toAmount(), paged.balanceAfter[last.id])
        assertEquals(880.toAmount(), byLabel.balanceAfter[last.id])
        assertEquals(880.toAmount(), searched.balanceAfter[last.id])
    }

    // The last line of a fully displayed period meets the balance at its close.
    @Test
    fun `shouldEndThePeriodOnItsClosingBalance`() {
        val userId = givenBooklet(
            income("Salaire", 2000, Month.FEBRUARY, 3),
            spent("Loyer", 850, Month.FEBRUARY, 5),
            spent("Courses", 60, Month.FEBRUARY, 28),
            spent("Mars", 30, Month.MARCH, 2),
        )

        val result = load(userId)

        val closing = factory.fakeBookletAt(bookletId).balanceAt(LocalDate.of(2025, 3, 1))
        assertEquals(closing, result.balanceAfter.values.last())
        assertEquals(2090.toAmount(), closing)
    }

    @Test
    fun `shouldFollowAPickedDateRange`() {
        val januaryRent = spent("Loyer", 850, Month.JANUARY, 5)
        val februaryGroceries = spent("Courses", 60, Month.FEBRUARY, 8)
        val userId = givenBooklet(spent("Décembre", 100, Month.JANUARY, 1), januaryRent, februaryGroceries)

        val result = load(userId, startDate = LocalDate.of(2025, 1, 3), endDate = LocalDate.of(2025, 2, 28))

        assertEquals(50.toAmount(), result.balanceAfter[januaryRent.id])
        assertEquals((-10).toAmount(), result.balanceAfter[februaryGroceries.id])
    }

    private fun givenBooklet(vararg transactions: Transaction): UserId {
        val booklet = BookletFixture.aBooklet(id = bookletId, label = "Compte courant", amount = 1000.toAmount())
        val ctx = scenario.withUser().withBooklet(booklet)
        factory.fakeTransactionRepository().initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), transactions.toMutableList()))
        factory.regularTransactionState.init(emptyList())
        return ctx.userId
    }

    private fun spent(label: String, amount: Int, month: Month, day: Int, preview: Boolean = false, enteredAt: LocalDateTime? = null) =
        transaction(label, amount, month, day, isIncome = false, preview = preview, enteredAt = enteredAt)

    private fun income(label: String, amount: Int, month: Month, day: Int) =
        transaction(label, amount, month, day, isIncome = true, preview = false, enteredAt = null)

    private fun transaction(label: String, amount: Int, month: Month, day: Int, isIncome: Boolean, preview: Boolean, enteredAt: LocalDateTime?) =
        Transaction(
            id = UUID.randomUUID(),
            label = label,
            date = LocalDate.of(2025, month, day),
            amount = amount.toAmount(),
            isIncome = isIncome,
            lastModified = enteredAt ?: LocalDate.of(2025, month, day).atStartOfDay(),
            isPreview = preview,
        )

    private fun load(
        userId: UserId,
        pageSize: Int = 50,
        pageNumber: Int = 0,
        sortField: TransactionSortField? = null,
        search: String? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
    ): BookletLoadingResult =
        factory.loadTransactionsForBookletForAMonthService.handle(
            LoadTransactionsForBookletForAMonthQuery(
                userId, bookletId, Month.FEBRUARY, 2025,
                startingMonth = Month.FEBRUARY, startingYear = 2025,
                startDate = startDate, endDate = endDate,
                pageNumber = pageNumber, pageSize = pageSize,
                sortDirection = TransactionSortDirection.ASCENDING.takeIf { sortField != null },
                sortField = sortField,
                search = search,
            ),
        ).mapNotNullOrFailure()!!
}
