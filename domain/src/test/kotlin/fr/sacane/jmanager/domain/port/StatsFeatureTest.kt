package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.BookletsByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserBooklet
import fr.sacane.jmanager.domain.fake.IdBookletByTransaction
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fake.UserTag
import fr.sacane.jmanager.domain.fixture.TransactionFixture
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.input.stats.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class StatsFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val getMonthlyBookletStatsUseCase: GetMonthlyBookletStatsUseCase = factory.getMonthlyBookletStatsService
    private val getCategoryDistributionUseCase: GetCategoryDistributionUseCase = factory.getCategoryDistributionService
    private val getTrendStatsUseCase: GetTrendStatsUseCase = factory.getTrendStatsService
    private val getPrevisionalTransactionsUseCase: GetPrevisionalTransactionsUseCase = factory.getPrevisionalTransactionsService
    private val getDailyTrendStatsUseCase: GetDailyTrendStatsUseCase = factory.getDailyTrendStatsService
    private val bookletState: State<BookletsByOwner> = factory.bookletState()
    private val transactionState = factory.fakeTransactionRepository()

    @AfterEach
    fun clear() {
        factory.clearAll()
    }

    private fun tx(label: String, amount: BigDecimal, isIncome: Boolean, date: LocalDate, isPreview: Boolean = false, tag: Tag? = null): Transaction {
        return TransactionFixture.aTransaction(label = label, amount = Amount(amount), isIncome = isIncome, date = date, isPreview = isPreview, tag = tag ?: factory.fakeTagRepository().defaultTag())
    }

    private fun txWithTag(label: String, amount: BigDecimal, date: LocalDate, tag: Tag): Transaction {
        return Transaction(id = UUID.randomUUID(), label = label, date = date, amount = Amount(amount), isIncome = amount >= BigDecimal.ZERO, tag = tag, isPreview = false)
    }

    @Nested
    inner class MonthlyBookletStatsTest {

        @Test
        fun `Should return monthly stats for a given booklet and year`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Salary", BigDecimal("2000"), true, LocalDate.of(2025, 1, 15)),
                tx("Rent", BigDecimal("-800"), false, LocalDate.of(2025, 1, 5)),
                tx("Groceries", BigDecimal("-200"), false, LocalDate.of(2025, 2, 10))
            )))

            getMonthlyBookletStatsUseCase.handle(GetMonthlyBookletStatsQuery(ctx.booklet.id!!, 2025, ctx.userId))
                .assertTrue { this.year == 2025 && this.monthlyData.size == 12 }
        }

        @Test
        fun `Should calculate correct income and expenses per month`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Salary", BigDecimal("500"), true, LocalDate.of(2025, 1, 15)),
                tx("Groceries", BigDecimal("-200"), false, LocalDate.of(2025, 1, 10))
            )))

            getMonthlyBookletStatsUseCase.handle(GetMonthlyBookletStatsQuery(ctx.booklet.id!!, 2025, ctx.userId))
                .assertTrue {
                    val januaryData = this.monthlyData.find { it.month == 1 }
                    januaryData != null
                            && januaryData.income == Amount(BigDecimal("500"))
                            && januaryData.expenses == Amount(BigDecimal("200"))
                }
        }

        @Test
        fun `Should return empty stats when no transactions exist for the year`() {
            val ctx = scenario.withUser().withBooklet()

            getMonthlyBookletStatsUseCase.handle(GetMonthlyBookletStatsQuery(ctx.booklet.id!!, 2025, ctx.userId))
                .assertTrue {
                    this.monthlyData.all {
                        it.income.value.compareTo(BigDecimal.ZERO) == 0 &&
                                it.expenses.value.compareTo(BigDecimal.ZERO) == 0
                    }
                }
        }

        @Test
        fun `Should fail when booklet does not exist`() {
            val ctx = scenario.withUser().withBooklet()

            val result = getMonthlyBookletStatsUseCase.handle(GetMonthlyBookletStatsQuery(UUID.randomUUID(), 2025, ctx.userId))

            result.assertFailure()
            assertEquals("domain.stats.monthly.booklet_not_found", result.errorInfo?.key)
        }

        @Test
        fun `Should not include previsional transactions in monthly stats`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Future Salary", BigDecimal("2000"), true, LocalDate.of(2025, 1, 15), isPreview = true),
                tx("Real Expense", BigDecimal("-100"), false, LocalDate.of(2025, 1, 10), isPreview = false)
            )))

            getMonthlyBookletStatsUseCase.handle(GetMonthlyBookletStatsQuery(ctx.booklet.id!!, 2025, ctx.userId))
                .assertTrue {
                    val januaryData = this.monthlyData.find { it.month == 1 }
                    januaryData != null && januaryData.income.value.compareTo(BigDecimal.ZERO) == 0
                }
        }
    }

    @Nested
    inner class CategoryDistributionStatsTest {

        @Test
        fun `Should return category distribution for all user transactions`() {
            val ctx = scenario.withUser().withBooklet()
            val foodTag = Tag.Personal(id = UUID.randomUUID(), label = "Food")
            factory.tagTestState().init(UserTag(ctx.userId, mutableListOf(foodTag)))
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                txWithTag("Groceries", BigDecimal("-100"), LocalDate.of(2025, 1, 10), foodTag),
                txWithTag("Restaurant", BigDecimal("-50"), LocalDate.of(2025, 1, 15), foodTag)
            )))

            getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(ctx.userId))
                .assertTrue { this.categories.isNotEmpty() }
        }

        @Test
        fun `Should calculate correct amounts per category`() {
            val ctx = scenario.withUser().withBooklet()
            val foodTag = Tag.Personal(id = UUID.randomUUID(), label = "Food")
            factory.tagTestState().init(UserTag(ctx.userId, mutableListOf(foodTag)))
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                txWithTag("Groceries", BigDecimal("-100"), LocalDate.of(2025, 1, 10), foodTag),
                txWithTag("Restaurant", BigDecimal("-200"), LocalDate.of(2025, 1, 15), foodTag)
            )))

            getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(ctx.userId))
                .assertTrue {
                    val foodCategory = this.categories.find { it.tagLabel == "Food" }
                    foodCategory != null && foodCategory.totalAmount == Amount(BigDecimal("300"))
                }
        }

        @Test
        fun `Should calculate correct percentages for each category`() {
            val ctx = scenario.withUser().withBooklet()
            val foodTag = Tag.Personal("Food", UUID.randomUUID())
            val transportTag = Tag.Personal("Transport", UUID.randomUUID())
            factory.tagTestState().init(UserTag(ctx.userId, mutableListOf(foodTag, transportTag)))
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                txWithTag("Groceries", BigDecimal("-500"), LocalDate.of(2025, 1, 10), foodTag),
                txWithTag("Gas", BigDecimal("-500"), LocalDate.of(2025, 1, 15), transportTag)
            )))

            getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(ctx.userId))
                .assertTrue {
                    val totalPercentage = this.categories.sumOf { it.percentage.toDouble() }
                    totalPercentage in 99.9..100.1
                }
        }

        @Test
        fun `Should return empty distribution when no transactions exist`() {
            val ctx = scenario.withUser().withBooklet()
            val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = ctx.user, id = UUID.randomUUID())
            bookletState.init(listOf(BookletsByOwner(listOf(booklet), ctx.userId)))

            getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(ctx.userId))
                .assertTrue { this.categories.isEmpty() }
        }

        @Test
        fun `Should group transactions without tags as uncategorized`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Untagged expense", BigDecimal("100"), false, LocalDate.of(2025, 1, 10))
            )))

            getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(ctx.userId))
                .assertTrue {
                    val uncategorized = this.categories.find { it.tagLabel == "Aucune" }
                    uncategorized != null && uncategorized.totalAmount.value > BigDecimal.ZERO
                }
        }

        @Test
        fun `Should only include expenses in category distribution`() {
            val ctx = scenario.withUser().withBooklet()
            val foodTag = Tag.Personal(id = UUID.randomUUID(), label = "Food")
            val salaryTag = Tag.Personal(id = UUID.randomUUID(), label = "Salary")
            factory.tagTestState().init(UserTag(ctx.userId, mutableListOf(foodTag, salaryTag)))
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                txWithTag("Groceries", BigDecimal("-100"), LocalDate.of(2025, 1, 10), foodTag),
                txWithTag("Monthly Salary", BigDecimal("2000"), LocalDate.of(2025, 1, 15), salaryTag)
            )))

            getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(ctx.userId))
                .assertTrue { this.categories.all { it.totalAmount.value >= BigDecimal.ZERO } }
        }

        @Test
        fun `Should scope category distribution to selected booklet`() {
            val ctx = scenario.withUser().withBooklet()
            val foodTag = Tag.Personal(id = UUID.randomUUID(), label = "Food")
            factory.tagTestState().init(UserTag(ctx.userId, mutableListOf(foodTag)))
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                txWithTag("Groceries", BigDecimal("-100"), LocalDate.of(2025, 1, 10), foodTag)
            )))

            val secondBooklet = Booklet(Amount.fromString("500", "€".asCurrency()), "booklet2", owner = ctx.user, id = UUID.randomUUID())
            bookletState.init(listOf(BookletsByOwner(listOf(secondBooklet), ctx.userId)))
            transactionState.init(listOf(IdBookletByTransaction(IdUserBooklet(ctx.userId, secondBooklet.id!!), mutableListOf(
                txWithTag("Restaurant", BigDecimal("-200"), LocalDate.of(2025, 1, 12), foodTag)
            ))))

            getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(ctx.userId, bookletId = ctx.booklet.id))
                .assertTrue { this.totalExpenses == Amount(BigDecimal("100")) }
        }

        @Test
        fun `Should fail category distribution when period is partially provided`() {
            val ctx = scenario.withUser().withBooklet()

            val result = getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(
                userId = ctx.userId, startDate = LocalDate.of(2025, 1, 1), endDate = null
            ))

            result.assertFailure()
            assertEquals("domain.stats.category_distribution.invalid_partial_date_range", result.errorInfo?.key)
        }
    }

    @Nested
    inner class TrendStatsTest {

        @Test
        fun `Should return trend for last 12 months`() {
            val ctx = scenario.withUser().withBooklet()
            val currentDate = LocalDate.now()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                *(0..11).map { tx("Transaction $it", BigDecimal("100"), true, currentDate.minusMonths(it.toLong())) }.toTypedArray()
            )))

            getTrendStatsUseCase.handle(GetTrendStatsQuery(ctx.userId))
                .assertTrue { this.monthlyTrends.size == 12 }
        }

        @Test
        fun `Should calculate balance evolution correctly`() {
            val ctx = scenario.withUser().withBooklet()
            val currentDate = LocalDate.now()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                *(0..11).map { tx("Income $it", BigDecimal("${100 * (it + 1)}"), true, currentDate.minusMonths(it.toLong())) }.toTypedArray()
            )))

            getTrendStatsUseCase.handle(GetTrendStatsQuery(ctx.userId))
                .assertTrue {
                    val firstMonth = this.monthlyTrends.first()
                    val lastMonth = this.monthlyTrends.last()
                    lastMonth.cumulativeBalance.value > firstMonth.cumulativeBalance.value
                }
        }

        @Test
        fun `Should include all user booklets in trend calculation`() {
            val ctx = scenario.withUser().withBooklet()
            val currentDate = LocalDate.now()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Transaction 1", BigDecimal("100"), true, currentDate)
            )))

            val booklet2 = Booklet(Amount.fromString("500", "€".asCurrency()), "booklet2", owner = ctx.user, id = UUID.randomUUID())
            val tx2 = tx("Transaction 2", BigDecimal("200"), true, currentDate)
            booklet2.addTransaction(tx2)
            bookletState.init(listOf(BookletsByOwner(listOf(booklet2), ctx.userId)))

            getTrendStatsUseCase.handle(GetTrendStatsQuery(ctx.userId))
                .assertTrue { this.monthlyTrends.all { it.totalBooklets == 2 } }
        }

        @Test
        fun `Should order months from oldest to most recent`() {
            val ctx = scenario.withUser().withBooklet()
            val currentDate = LocalDate.now()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                *(0..11).map { tx("Transaction $it", BigDecimal("100"), true, currentDate.minusMonths(it.toLong())) }.toTypedArray()
            )))

            getTrendStatsUseCase.handle(GetTrendStatsQuery(ctx.userId))
                .assertTrue {
                    val months = this.monthlyTrends.map { it.month to it.year }
                    months == months.sortedWith(compareBy({ it.second }, { it.first }))
                }
        }

        @Test
        fun `Should handle months with no transactions`() {
            val ctx = scenario.withUser().withBooklet()
            val currentDate = LocalDate.now()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Transaction 1", BigDecimal("100"), true, currentDate.minusMonths(10)),
                tx("Transaction 2", BigDecimal("100"), true, currentDate.minusMonths(5))
            )))

            getTrendStatsUseCase.handle(GetTrendStatsQuery(ctx.userId))
                .assertTrue {
                    this.monthlyTrends.any {
                        it.income.value.compareTo(BigDecimal.ZERO) == 0 && it.expenses.value.compareTo(BigDecimal.ZERO) == 0
                    }
                }
        }

        @Test
        fun `Should scope trend stats to selected booklet and period`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("A1 Jan", BigDecimal("100"), true, LocalDate.of(2025, 1, 5)),
                tx("A1 Feb", BigDecimal("100"), true, LocalDate.of(2025, 2, 5))
            )))

            getTrendStatsUseCase.handle(GetTrendStatsQuery(
                userId = ctx.userId, bookletId = ctx.booklet.id,
                startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 2, 28)
            )).assertTrue {
                this.monthlyTrends.size == 2 && this.monthlyTrends.all { it.totalBooklets == 1 }
            }
        }

        @Test
        fun `Should fail trend stats when period is partially provided`() {
            val ctx = scenario.withUser().withBooklet()

            val result = getTrendStatsUseCase.handle(GetTrendStatsQuery(
                userId = ctx.userId, startDate = LocalDate.of(2025, 1, 1), endDate = null
            ))

            result.assertFailure()
            assertEquals("domain.stats.trend.invalid_partial_date_range", result.errorInfo?.key)
        }
    }

    @Nested
    inner class PrevisionalTransactionsTest {

        @Test
        fun `Should return previsional transactions within date range`() {
            val ctx = scenario.withUser().withBooklet()
            val startDate = LocalDate.now()
            val endDate = startDate.plusMonths(3)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Future Rent", BigDecimal("-800"), false, startDate.plusDays(15), isPreview = true),
                tx("Future Salary", BigDecimal("2000"), true, startDate.plusMonths(1), isPreview = true),
                tx("Future Bill", BigDecimal("-100"), false, startDate.plusMonths(2), isPreview = true)
            )))

            getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(ctx.userId, startDate, endDate))
                .assertTrue { this.transactions.all { it.date >= startDate && it.date <= endDate } }
        }

        @Test
        fun `Should only include transactions marked as previsional`() {
            val ctx = scenario.withUser().withBooklet()
            val startDate = LocalDate.now()
            val endDate = startDate.plusMonths(3)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Future Rent", BigDecimal("-800"), false, startDate.plusDays(15), isPreview = true),
                tx("Real Expense", BigDecimal("-100"), false, startDate.plusDays(10), isPreview = false),
                tx("Future Salary", BigDecimal("2000"), true, startDate.plusMonths(1), isPreview = true)
            )))

            getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(ctx.userId, startDate, endDate))
                .assertTrue { this.transactions.all { it.isPreview } }
        }

        @Test
        fun `Should group previsional transactions by booklet`() {
            val ctx = scenario.withUser().withBooklet()
            val startDate = LocalDate.now()
            val endDate = startDate.plusMonths(3)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Future Rent", BigDecimal("800"), false, startDate.plusDays(15), isPreview = true)
            )))

            val booklet2 = Booklet(Amount.fromString("500", "€".asCurrency()), "booklet2", owner = ctx.user, id = UUID.randomUUID())
            bookletState.init(listOf(BookletsByOwner(listOf(booklet2), ctx.userId)))
            transactionState.init(listOf(IdBookletByTransaction(IdUserBooklet(ctx.userId, booklet2.id!!), mutableListOf(
                tx("Future Bill", BigDecimal("100"), false, startDate.plusMonths(1), isPreview = true)
            ))))

            getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(ctx.userId, startDate, endDate))
                .assertTrue { this.groupedByBooklet.keys.size == 2 }
        }

        @Test
        fun `Should calculate total previsional amount`() {
            val ctx = scenario.withUser().withBooklet()
            val startDate = LocalDate.now()
            val endDate = startDate.plusMonths(3)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Future Salary", BigDecimal("2000"), true, startDate.plusMonths(1), isPreview = true),
                tx("Future Rent", BigDecimal("-800"), false, startDate.plusDays(15), isPreview = true)
            )))

            getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(ctx.userId, startDate, endDate))
                .assertTrue { this.totalAmount.value != BigDecimal.ZERO }
        }

        @Test
        fun `Should fail when start date is after end date`() {
            val ctx = scenario.withUser().withBooklet()
            val startDate = LocalDate.now().plusMonths(6)
            val endDate = LocalDate.now()

            val result = getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(ctx.userId, startDate, endDate))

            result.assertFailure()
            assertEquals("domain.stats.previsional.invalid_date_range", result.errorInfo?.key)
        }

        @Test
        fun `Should return empty result when no previsional transactions exist`() {
            val ctx = scenario.withUser().withBooklet()
            val startDate = LocalDate.now()
            val endDate = startDate.plusMonths(3)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf()))

            getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(ctx.userId, startDate, endDate))
                .assertTrue { this.transactions.isEmpty() }
        }

        @Test
        fun `Should sort previsional transactions by date`() {
            val ctx = scenario.withUser().withBooklet()
            val startDate = LocalDate.now()
            val endDate = startDate.plusMonths(3)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Future 3", BigDecimal("-100"), false, startDate.plusMonths(2), isPreview = true),
                tx("Future 1", BigDecimal("-800"), false, startDate.plusDays(15), isPreview = true),
                tx("Future 2", BigDecimal("2000"), true, startDate.plusMonths(1), isPreview = true)
            )))

            getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(ctx.userId, startDate, endDate))
                .assertTrue {
                    val dates = this.transactions.map { it.date }
                    dates == dates.sorted()
                }
        }

        @Test
        fun `Should scope previsional transactions to selected booklet`() {
            val ctx = scenario.withUser().withBooklet()
            val startDate = LocalDate.now()
            val endDate = startDate.plusMonths(3)
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("A1 Future", BigDecimal("-100"), false, startDate.plusDays(5), isPreview = true)
            )))

            val secondBooklet = Booklet(Amount.fromString("500", "€".asCurrency()), "booklet2", owner = ctx.user, id = UUID.randomUUID())
            bookletState.init(listOf(BookletsByOwner(listOf(secondBooklet), ctx.userId)))
            transactionState.init(listOf(IdBookletByTransaction(IdUserBooklet(ctx.userId, secondBooklet.id!!), mutableListOf(
                tx("A2 Future", BigDecimal("-200"), false, startDate.plusDays(10), isPreview = true)
            ))))

            getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(ctx.userId, startDate, endDate, ctx.booklet.id))
                .assertTrue { this.groupedByBooklet.keys.size == 1 }
        }

        @Test
        fun `Should split previsional transactions by regular and non regular`() {
            val ctx = scenario.withUser().withBooklet()
            val startDate = LocalDate.now()
            val endDate = startDate.plusMonths(1)

            val regularPreview = tx("Loyer régulier", BigDecimal("-800"), false, startDate.plusDays(5), isPreview = true)
                .copy(regularTransactionId = RegularTransactionId(UUID.randomUUID().toString()))
            val nonRegularPreview = tx("Facture ponctuelle", BigDecimal("-120"), false, startDate.plusDays(8), isPreview = true)

            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(regularPreview, nonRegularPreview)))

            getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(ctx.userId, startDate, endDate))
                .assertTrue {
                    this.regularTransactions.size == 1 && this.nonRegularTransactions.size == 1
                }
        }
    }

    @Nested
    inner class DailyTrendStatsTest {

        @Test
        fun `Should return daily trends for a standard month`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Salary", BigDecimal("2000"), true, LocalDate.of(2025, 1, 5)),
                tx("Groceries", BigDecimal("100"), false, LocalDate.of(2025, 1, 12))
            )))

            getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                userId = ctx.userId, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 31), bookletId = ctx.booklet.id
            )).assertTrue { this.dailyTrends.size == 31 }
        }

        @Test
        fun `Should return daily trends for cross-month custom cycle`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Income", BigDecimal("500"), true, LocalDate.of(2025, 5, 27)),
                tx("Expense", BigDecimal("200"), false, LocalDate.of(2025, 6, 3))
            )))

            getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                userId = ctx.userId, startDate = LocalDate.of(2025, 5, 25), endDate = LocalDate.of(2025, 6, 24), bookletId = ctx.booklet.id
            )).assertTrue {
                this.dailyTrends.size == 31 &&
                    this.dailyTrends.first().date == LocalDate.of(2025, 5, 25) &&
                    this.dailyTrends.last().date == LocalDate.of(2025, 6, 24)
            }
        }

        @Test
        fun `Should exclude preview transactions from daily trends`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Real", BigDecimal("100"), true, LocalDate.of(2025, 1, 5), isPreview = false),
                tx("Preview", BigDecimal("999"), true, LocalDate.of(2025, 1, 5), isPreview = true)
            )))

            getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                userId = ctx.userId, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 31), bookletId = ctx.booklet.id
            )).assertTrue {
                val day5 = this.dailyTrends.find { it.date == LocalDate.of(2025, 1, 5) }
                day5 != null && day5.income == Amount(BigDecimal("100"))
            }
        }

        @Test
        fun `Should return zero entries when no transactions in range`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf()))

            getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                userId = ctx.userId, startDate = LocalDate.of(2025, 3, 1), endDate = LocalDate.of(2025, 3, 31), bookletId = ctx.booklet.id
            )).assertTrue {
                this.dailyTrends.size == 31 &&
                    this.dailyTrends.all { it.income.value.compareTo(BigDecimal.ZERO) == 0 }
            }
        }

        @Test
        fun `Should fail when start date is after end date`() {
            val ctx = scenario.withUser().withBooklet()

            val result = getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                userId = ctx.userId, startDate = LocalDate.of(2025, 6, 1), endDate = LocalDate.of(2025, 5, 1), bookletId = ctx.booklet.id
            ))

            result.assertFailure()
            assertEquals("domain.stats.daily_trend.invalid_date_range", result.errorInfo?.key)
        }

        @Test
        fun `cumulative balance is seeded from booklet balance before period start`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Pre-period income", BigDecimal("500"), true, LocalDate.of(2024, 12, 31)),
                tx("In-period expense", BigDecimal("200"), false, LocalDate.of(2025, 1, 5))
            )))

            // booklet initialSold = 0, pre-period income = +500 => startingBalance = 500
            // day 1: no in-period tx => cumulative = 500
            // day 5: expense 200 => cumulative = 300
            getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                userId = ctx.userId, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 31), bookletId = ctx.booklet.id
            )).assertTrue {
                val day1 = this.dailyTrends.find { it.date == LocalDate.of(2025, 1, 1) }
                val day5 = this.dailyTrends.find { it.date == LocalDate.of(2025, 1, 5) }
                day1 != null && day1.cumulativeBalance.value.compareTo(BigDecimal("500")) == 0 &&
                    day5 != null && day5.cumulativeBalance.value.compareTo(BigDecimal("300")) == 0
            }
        }

        @Test
        fun `cumulative balance aggregates starting balances of all booklets`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("B1 pre-period", BigDecimal("300"), true, LocalDate.of(2024, 12, 31))
            )))

            val booklet2 = Booklet(Amount(BigDecimal("200")), "booklet2", owner = ctx.user, id = UUID.randomUUID())
            bookletState.init(listOf(BookletsByOwner(listOf(booklet2), ctx.userId)))
            transactionState.init(listOf(IdBookletByTransaction(IdUserBooklet(ctx.userId, booklet2.id!!), mutableListOf(
                tx("B2 pre-period", BigDecimal("100"), true, LocalDate.of(2024, 12, 31))
            ))))

            // booklet1 initialSold=0 + 300 = 300, booklet2 initialSold=200 + 100 = 300
            // combined startingBalance = 600
            getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                userId = ctx.userId, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 31), bookletId = null
            )).assertTrue {
                val day1 = this.dailyTrends.find { it.date == LocalDate.of(2025, 1, 1) }
                day1 != null && day1.cumulativeBalance.value.compareTo(BigDecimal("600")) == 0
            }
        }

        @Test
        fun `Should scope daily trends to selected booklet`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("B1 Income", BigDecimal("300"), true, LocalDate.of(2025, 1, 5))
            )))

            getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                userId = ctx.userId, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 31), bookletId = ctx.booklet.id
            )).assertTrue { this.dailyTrends.all { it.totalBooklets == 1 } }
        }

        @Test
        fun `Should compute cumulative balance correctly across days`() {
            val ctx = scenario.withUser().withBooklet()
            transactionState.initWith(IdBookletByTransaction(IdUserBooklet(ctx.userId, ctx.booklet.id!!), mutableListOf(
                tx("Income", BigDecimal("1000"), true, LocalDate.of(2025, 1, 1)),
                tx("Expense", BigDecimal("300"), false, LocalDate.of(2025, 1, 10))
            )))

            getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                userId = ctx.userId, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 31), bookletId = ctx.booklet.id
            )).assertTrue {
                val day1 = this.dailyTrends.find { it.date == LocalDate.of(2025, 1, 1) }
                val day10 = this.dailyTrends.find { it.date == LocalDate.of(2025, 1, 10) }
                val day31 = this.dailyTrends.find { it.date == LocalDate.of(2025, 1, 31) }
                day1 != null && day1.cumulativeBalance.value.compareTo(BigDecimal("1000")) == 0 &&
                    day10 != null && day10.cumulativeBalance.value.compareTo(BigDecimal("700")) == 0 &&
                    day31 != null && day31.cumulativeBalance.value.compareTo(BigDecimal("700")) == 0
            }
        }
    }
}
