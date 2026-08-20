package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fake.UserRegularTransaction
import fr.sacane.jmanager.domain.fixture.BookletFixture
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.input.booklet.FindRegenerableTransactionsQuery
import fr.sacane.jmanager.domain.toAmount
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class RegenerableTransactionsFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val findRegenerableTransactionsService = factory.findRegenerableTransactionsService

    @AfterEach
    fun clear() {
        factory.clearAll()
    }

    private fun aRegularTransaction(
        userId: UserId,
        suffix: String,
        label: String = "Loyer",
        amount: Amount = 900.toAmount(),
        recurrenceRule: RecurrenceRule = RecurrenceRule.Monthly(5),
        startDate: LocalDate = LocalDate.of(2024, 1, 1)
    ): RegularTransaction = RegularTransaction(
        label = label,
        amount = amount,
        isIncome = false,
        id = RegularTransactionId("${userId.value}-$suffix"),
        startDate = startDate,
        frequencyProperty = FrequencyProperty.Forever(),
        recurrenceRule = recurrenceRule
    )

    private fun register(userId: UserId, bookletId: UUID, vararg regularTransactions: RegularTransaction) {
        factory.regularTransactionState.init(
            regularTransactions.map {
                UserRegularTransaction(userId = userId, transaction = it, bookletIds = listOf(bookletId))
            }
        )
        factory.fakeTransactionRepository().init(emptyList())
    }

    @Test
    fun `should expose label amount and date of each excluded occurrence for the current month`() {
        val currentDate = LocalDate.now()
        val ctx = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "Candidates Booklet", amount = 1000.toAmount()))
        val bookletId = ctx.booklet.id!!
        val rent = aRegularTransaction(ctx.userId, "rent")
        register(ctx.userId, bookletId, rent)
        factory.trackerRepository().markMonthAsExcluded(rent.id, bookletId, currentDate.year, currentDate.month)

        val result = findRegenerableTransactionsService.handle(
            FindRegenerableTransactionsQuery(
                userId = ctx.userId, bookletId = bookletId,
                month = currentDate.month, year = currentDate.year
            )
        )

        result.assertTrue { size == 1 }
        result.assertTrue { single().regularTransactionId == rent.id }
        result.assertTrue { single().label == "Loyer" }
        result.assertTrue { single().amount == 900.toAmount() }
        result.assertTrue { !single().isIncome }
        result.assertTrue { single().date == currentDate.withDayOfMonth(5) }
    }

    @Test
    fun `should list a future month without persisting anything nor touching the tracker`() {
        val futureDate = LocalDate.now().plusMonths(2)
        val ctx = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "Future Candidates", amount = 1000.toAmount()))
        val bookletId = ctx.booklet.id!!
        val rent = aRegularTransaction(ctx.userId, "future-rent")
        register(ctx.userId, bookletId, rent)
        factory.trackerRepository().markMonthAsExcluded(rent.id, bookletId, futureDate.year, futureDate.month)

        val result = findRegenerableTransactionsService.handle(
            FindRegenerableTransactionsQuery(
                userId = ctx.userId, bookletId = bookletId,
                month = futureDate.month, year = futureDate.year
            )
        )

        result.assertTrue { size == 1 }
        result.assertTrue { single().date == futureDate.withDayOfMonth(5) }
        val persisted = factory.fakeTransactionRepository().getStates().flatMap { it.transactions }
        assertTrue(persisted.isEmpty(), "Listing candidates must not persist any transaction")
        val tracker = factory.trackerRepository().findTracker(rent.id, bookletId)
        assertTrue(
            tracker?.excludedMonths?.contains(YearMonth.from(futureDate)) == true,
            "Listing candidates must leave the exclusion untouched"
        )
    }

    @Test
    fun `should list one candidate per occurrence for a weekly recurrence`() {
        val targetYearMonth = YearMonth.now().plusMonths(1)
        val ctx = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "Weekly Candidates", amount = 1000.toAmount()))
        val bookletId = ctx.booklet.id!!
        val groceries = aRegularTransaction(
            ctx.userId, "weekly", label = "Courses", amount = 60.toAmount(),
            recurrenceRule = RecurrenceRule.Weekly(setOf(DayOfWeek.MONDAY))
        )
        register(ctx.userId, bookletId, groceries)
        factory.trackerRepository().markMonthAsExcluded(groceries.id, bookletId, targetYearMonth.year, targetYearMonth.month)
        val expectedMondays = (1..targetYearMonth.lengthOfMonth())
            .map { targetYearMonth.atDay(it) }
            .count { it.dayOfWeek == DayOfWeek.MONDAY }

        val result = findRegenerableTransactionsService.handle(
            FindRegenerableTransactionsQuery(
                userId = ctx.userId, bookletId = bookletId,
                month = targetYearMonth.month, year = targetYearMonth.year
            )
        )

        result.assertTrue { size == expectedMondays }
        result.assertTrue { all { candidate -> candidate.date.dayOfWeek == DayOfWeek.MONDAY } }
        result.assertTrue { all { candidate -> candidate.regularTransactionId == groceries.id } }
    }

    @Test
    fun `should return an empty list when no month is excluded`() {
        val currentDate = LocalDate.now()
        val ctx = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "No Candidates", amount = 1000.toAmount()))
        val bookletId = ctx.booklet.id!!
        register(ctx.userId, bookletId, aRegularTransaction(ctx.userId, "not-excluded"))

        val result = findRegenerableTransactionsService.handle(
            FindRegenerableTransactionsQuery(
                userId = ctx.userId, bookletId = bookletId,
                month = currentDate.month, year = currentDate.year
            )
        )

        result.assertTrue { isEmpty() }
    }

    @Test
    fun `should return an empty list for a past month`() {
        val pastYearMonth = YearMonth.of(2024, Month.MARCH)
        val ctx = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "Past Candidates", amount = 1000.toAmount()))
        val bookletId = ctx.booklet.id!!
        val rent = aRegularTransaction(ctx.userId, "past-rent")
        register(ctx.userId, bookletId, rent)
        factory.trackerRepository().markMonthAsExcluded(rent.id, bookletId, pastYearMonth.year, pastYearMonth.month)

        val result = findRegenerableTransactionsService.handle(
            FindRegenerableTransactionsQuery(
                userId = ctx.userId, bookletId = bookletId,
                month = pastYearMonth.month, year = pastYearMonth.year
            )
        )

        result.assertTrue { isEmpty() }
    }

    @Test
    fun `should only list the regular transactions that are actually excluded`() {
        val currentDate = LocalDate.now()
        val ctx = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "Mixed Candidates", amount = 1000.toAmount()))
        val bookletId = ctx.booklet.id!!
        val rent = aRegularTransaction(ctx.userId, "mixed-rent", label = "Loyer")
        val salary = aRegularTransaction(ctx.userId, "mixed-salary", label = "Salaire", recurrenceRule = RecurrenceRule.Monthly(28))
        register(ctx.userId, bookletId, rent, salary)
        factory.trackerRepository().markMonthAsExcluded(rent.id, bookletId, currentDate.year, currentDate.month)

        val result = findRegenerableTransactionsService.handle(
            FindRegenerableTransactionsQuery(
                userId = ctx.userId, bookletId = bookletId,
                month = currentDate.month, year = currentDate.year
            )
        )

        result.assertTrue { size == 1 }
        result.assertTrue { single().label == "Loyer" }
    }

    @Test
    fun `should fail when the booklet belongs to another user`() {
        val currentDate = LocalDate.now()
        val owner = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "Someone Else Booklet", amount = 1000.toAmount()))
        val bookletId = owner.booklet.id!!
        val rent = aRegularTransaction(owner.userId, "foreign-rent")
        register(owner.userId, bookletId, rent)
        factory.trackerRepository().markMonthAsExcluded(rent.id, bookletId, currentDate.year, currentDate.month)
        val intruder = scenario.withUser(
            UserFixture.aUserWithPassword(user = UserFixture.aUser(username = "intruder", email = "intruder@jmanager.fr"))
        )

        val result = findRegenerableTransactionsService.handle(
            FindRegenerableTransactionsQuery(
                userId = intruder.userId, bookletId = bookletId,
                month = currentDate.month, year = currentDate.year
            )
        )

        result.assertFailure(ResultState.BOOKLET_NOT_FOUND)
    }

    @Test
    fun `should fail when the booklet does not exist`() {
        val ctx = scenario.withUser()
        factory.regularTransactionState.init(emptyList())

        val result = findRegenerableTransactionsService.handle(
            FindRegenerableTransactionsQuery(
                userId = ctx.userId, bookletId = UUID.randomUUID(),
                month = Month.JANUARY, year = 2030
            )
        )

        result.assertFailure(ResultState.BOOKLET_NOT_FOUND)
    }
}
