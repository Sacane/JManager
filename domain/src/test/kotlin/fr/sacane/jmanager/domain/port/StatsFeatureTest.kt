package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.BookletsByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserBooklet
import fr.sacane.jmanager.domain.fake.IdBookletByTransaction
import fr.sacane.jmanager.domain.fake.UserTag
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.input.stats.*
import fr.sacane.jmanager.domain.utils.Result
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class StatsFeatureTest : FeatureTest() {

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

    @Nested
    inner class MonthlyBookletStatsTest {

        @Test
        fun `Should return monthly stats for a given booklet and year`() {
            launchWithUserId {
                val transactions = listOf(
                    generateTransaction("Salary", Amount(BigDecimal("2000")), true, LocalDate.of(2025, 1, 15)),
                    generateTransaction("Rent", Amount(BigDecimal("-800")), false, LocalDate.of(2025, 1, 5)),
                    generateTransaction("Groceries", Amount(BigDecimal("-200")), false, LocalDate.of(2025, 2, 10))
                )
                initTransactions(transactions)

                getMonthlyBookletStatsUseCase.handle(GetMonthlyBookletStatsQuery(booklet.id!!, 2025, userId))
                    .assertTrue {
                        this.year == 2025 && this.monthlyData.size == 12
                    }
            }
        }

        @Test
        fun `Should calculate correct income and expenses per month`() {
            launchWithUserId {
                val transactions = listOf(
                    generateTransaction("Salary", Amount(BigDecimal("500")), true, LocalDate.of(2025, 1, 15)),
                    generateTransaction("Groceries", Amount(BigDecimal("-200")), false, LocalDate.of(2025, 1, 10))
                )
                initTransactions(transactions)

                getMonthlyBookletStatsUseCase.handle(GetMonthlyBookletStatsQuery(booklet.id!!, 2025, userId))
                    .assertTrue {
                        val januaryData = this.monthlyData.find { it.month == 1 }
                        januaryData != null
                                && januaryData.income == Amount(BigDecimal("500"))
                                && januaryData.expenses == Amount(BigDecimal("200"))
                    }
            }
        }

        @Test
        fun `Should return empty stats when no transactions exist for the year`() {
            launchWithUserId {
                getMonthlyBookletStatsUseCase.handle(GetMonthlyBookletStatsQuery(booklet.id!!, 2025, userId))
                    .assertTrue {
                        this.monthlyData.all {
                            it.income.value.compareTo(BigDecimal.ZERO) == 0 &&
                                    it.expenses.value.compareTo(BigDecimal.ZERO) == 0
                        }
                    }
            }
        }

        @Test
        fun `Should fail when booklet does not exist`() {
            launchWithUserId {
                val result = getMonthlyBookletStatsUseCase.handle(GetMonthlyBookletStatsQuery(UUID.randomUUID(), 2025, userId))

                result.assertFailure()
                assertEquals("domain.stats.monthly.booklet_not_found", result.errorInfo?.key)
            }
        }

        @Test
        fun `Should not include previsional transactions in monthly stats`() {
            launchWithUserId {
                val transactions = listOf(
                    generateTransaction("Future Salary", Amount(BigDecimal("2000")), true, LocalDate.of(2025, 1, 15), isPreview = true),
                    generateTransaction("Real Expense", Amount(BigDecimal("-100")), false, LocalDate.of(2025, 1, 10), isPreview = false)
                )
                initTransactions(transactions)

                getMonthlyBookletStatsUseCase.handle(GetMonthlyBookletStatsQuery(booklet.id!!, 2025, userId))
                    .assertTrue {
                        val januaryData = this.monthlyData.find { it.month == 1 }
                        januaryData != null && januaryData.income.value.compareTo(BigDecimal.ZERO) == 0
                    }
            }
        }
    }

    @Nested
    inner class CategoryDistributionStatsTest {

        @Test
        fun `Should return category distribution for all user transactions`() {
            launchWithUserId {
                val foodTag = Tag.Personal(id = UUID.randomUUID(), label = "Food")
                initTags(listOf(UserTag(userId, mutableListOf(foodTag))))
                val transactions = listOf(
                    generateTransactionWithTag("Groceries", Amount(BigDecimal("-100")), LocalDate.of(2025, 1, 10), foodTag),
                    generateTransactionWithTag("Restaurant", Amount(BigDecimal("-50")), LocalDate.of(2025, 1, 15), foodTag)
                )
                initTransactions(transactions)

                getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(userId))
                    .assertTrue {
                        this.categories.isNotEmpty()
                    }
            }
        }

        @Test
        fun `Should calculate correct amounts per category`() {
            launchWithUserId {
                val foodTag = Tag.Personal(id = UUID.randomUUID(), label = "Food")
                initTags(listOf(UserTag(userId, mutableListOf(foodTag))))

                val transactions = listOf(
                    generateTransactionWithTag("Groceries", Amount(BigDecimal("-100")), LocalDate.of(2025, 1, 10), foodTag),
                    generateTransactionWithTag("Restaurant", Amount(BigDecimal("-200")), LocalDate.of(2025, 1, 15), foodTag)
                )
                initTransactions(transactions)

                getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(userId))
                    .assertTrue {
                        val foodCategory = this.categories.find { it.tagLabel == "Food" }
                        foodCategory != null && foodCategory.totalAmount == Amount(BigDecimal("300"))
                    }
            }
        }

        @Test
        fun `Should calculate correct percentages for each category`() {
            launchWithUserId {
                val foodTag = Tag.Personal("Food", UUID.randomUUID())
                val transportTag = Tag.Personal("Transport",UUID.randomUUID())
                initTags(listOf(UserTag(userId, mutableListOf(foodTag, transportTag))))

                val transactions = listOf(
                    generateTransactionWithTag("Groceries", Amount(BigDecimal("-500")), LocalDate.of(2025, 1, 10), foodTag),
                    generateTransactionWithTag("Gas", Amount(BigDecimal("-500")), LocalDate.of(2025, 1, 15), transportTag)
                )
                initTransactions(transactions)

                getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(userId))
                    .assertTrue {
                        val totalPercentage = this.categories.sumOf { it.percentage.toDouble() }
                        totalPercentage in 99.9..100.1
                    }
            }
        }

        @Test
        fun `Should return empty distribution when no transactions exist`() {
            launchWithUserId {
                val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = User(userId, "John", null), id = UUID.randomUUID())
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), userId)))

                getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(userId))
                    .assertTrue {
                        this.categories.isEmpty()
                    }
            }
        }

        @Test
        fun `Should group transactions without tags as uncategorized`() {
            launchWithUserId {
                val transactions = listOf(
                    generateTransaction("Untagged expense", Amount(BigDecimal("100")), false, LocalDate.of(2025, 1, 10))
                )
                initTransactions(transactions)

                val result = getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(userId))

                result.assertTrue {
                    val uncategorized = this.categories.find { it.tagLabel == "Aucune" }
                    uncategorized != null && uncategorized.totalAmount.value > BigDecimal.ZERO
                }
            }
        }

        @Test
        fun `Should only include expenses in category distribution`() {
            launchWithUserId {
                val foodTagId = UUID.randomUUID()
                val salaryTagId = UUID.randomUUID()
                val foodTag = Tag.Personal(id = foodTagId, label = "Food")
                val salaryTag = Tag.Personal(id = salaryTagId, label = "Salary")

                initTags(listOf(UserTag(userId, mutableListOf(foodTag, salaryTag))))

                val transactions = listOf(
                    generateTransactionWithTag("Groceries", Amount(BigDecimal("-100")), LocalDate.of(2025, 1, 10), foodTag),
                    generateTransactionWithTag("Monthly Salary", Amount(BigDecimal("2000")), LocalDate.of(2025, 1, 15), salaryTag)
                )
                initTransactions(transactions)

                getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(userId))
                    .assertTrue {
                        this.categories.all { it.totalAmount.value >= BigDecimal.ZERO }
                    }
            }
        }

        @Test
        fun `Should scope category distribution to selected booklet`() {
            launchWithUserId {
                val foodTag = Tag.Personal(id = UUID.randomUUID(), label = "Food")
                initTags(listOf(UserTag(userId, mutableListOf(foodTag))))

                val secondBooklet = Booklet(
                    Amount.fromString("500", "€".asCurrency()),
                    "booklet2",
                    owner = User(userId, "John", null),
                    id = UUID.randomUUID()
                )

                val firstBookletTransaction = generateTransactionWithTag(
                    "Groceries",
                    Amount(BigDecimal("-100")),
                    LocalDate.of(2025, 1, 10),
                    foodTag
                )
                initTransactions(listOf(firstBookletTransaction))

                val secondBookletTransaction = generateTransactionWithTag(
                    "Restaurant",
                    Amount(BigDecimal("-200")),
                    LocalDate.of(2025, 1, 12),
                    foodTag
                )
                bookletState.init(listOf(BookletsByOwner(listOf(secondBooklet), userId)))
                transactionState.init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(userId, secondBooklet.id!!),
                            mutableListOf(secondBookletTransaction)
                        )
                    )
                )

                getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(userId, bookletId = booklet.id))
                    .assertTrue {
                        this.totalExpenses == Amount(BigDecimal("100"))
                    }
            }
        }

        @Test
        fun `Should fail category distribution when period is partially provided`() {
            launchWithUserId {
                val result = getCategoryDistributionUseCase.handle(GetCategoryDistributionQuery(
                    userId = userId,
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = null
                ))

                result.assertFailure()
                assertEquals("domain.stats.category_distribution.invalid_partial_date_range", result.errorInfo?.key)
            }
        }
    }

    @Nested
    inner class TrendStatsTest {

        @Test
        fun `Should return trend for last 12 months`() {
            launchWithUserId {
                val currentDate = LocalDate.now()
                val transactions = (0..11).map { monthsAgo ->
                    val date = currentDate.minusMonths(monthsAgo.toLong())
                    generateTransaction("Transaction $monthsAgo", Amount(BigDecimal("100")), true, date)
                }
                initTransactions(transactions)

                getTrendStatsUseCase.handle(GetTrendStatsQuery(userId))
                    .assertTrue {
                        this.monthlyTrends.size == 12
                    }
            }
        }

        @Test
        fun `Should calculate balance evolution correctly`() {
            launchWithUserId {
                val currentDate = LocalDate.now()
                val transactions = (0..11).map { monthsAgo ->
                    val date = currentDate.minusMonths(monthsAgo.toLong())
                    generateTransaction("Income $monthsAgo", Amount(BigDecimal("${100 * (monthsAgo + 1)}")), true, date)
                }
                initTransactions(transactions)

                getTrendStatsUseCase.handle(GetTrendStatsQuery(userId))
                    .assertTrue {
                        val firstMonth = this.monthlyTrends.first()
                        val lastMonth = this.monthlyTrends.last()
                        lastMonth.cumulativeBalance.value > firstMonth.cumulativeBalance.value
                    }
            }
        }

        @Test
        fun `Should include all user booklets in trend calculation`() {
            launchWithUserId {
                val currentDate = LocalDate.now()
                val transactions1 = listOf(
                    generateTransaction("Transaction 1", Amount(BigDecimal("100")), true, currentDate)
                )
                initTransactions(transactions1)

                val booklet2 = Booklet(
                    Amount.fromString("500", "€".asCurrency()),
                    "booklet2",
                    owner = User(userId, "John", null),
                    id = UUID.randomUUID()
                )
                val transactions2 = listOf(
                    generateTransaction("Transaction 2", Amount(BigDecimal("200")), true, currentDate)
                )
                transactions2.forEach { booklet2.addTransaction(it) }

                bookletState.init(listOf(BookletsByOwner(listOf(booklet2), userId)))

                getTrendStatsUseCase.handle(GetTrendStatsQuery(userId))
                    .assertTrue {
                        this.monthlyTrends.all { it.totalBooklets == 2 }
                    }
            }
        }

        @Test
        fun `Should order months from oldest to most recent`() {
            launchWithUserId {
                val currentDate = LocalDate.now()
                val transactions = (0..11).map { monthsAgo ->
                    val date = currentDate.minusMonths(monthsAgo.toLong())
                    generateTransaction("Transaction $monthsAgo", Amount(BigDecimal("100")), true, date)
                }
                initTransactions(transactions)

                getTrendStatsUseCase.handle(GetTrendStatsQuery(userId))
                    .assertTrue {
                        val months = this.monthlyTrends.map { it.month to it.year }
                        months == months.sortedWith(compareBy({ it.second }, { it.first }))
                    }
            }
        }

        @Test
        fun `Should handle months with no transactions`() {
            launchWithUserId {
                val currentDate = LocalDate.now()
                val transactions = listOf(
                    generateTransaction("Transaction 1", Amount(BigDecimal("100")), true, currentDate.minusMonths(10)),
                    generateTransaction("Transaction 2", Amount(BigDecimal("100")), true, currentDate.minusMonths(5))
                )
                initTransactions(transactions)

                getTrendStatsUseCase.handle(GetTrendStatsQuery(userId))
                    .assertTrue {
                        this.monthlyTrends.any {
                            it.income.value.compareTo(BigDecimal.ZERO) == 0 && it.expenses.value.compareTo(BigDecimal.ZERO) == 0
                        }
                    }
            }
        }

        @Test
        fun `Should scope trend stats to selected booklet and period`() {
            launchWithUserId {
                val janDate = LocalDate.of(2025, 1, 5)
                val febDate = LocalDate.of(2025, 2, 5)
                initTransactions(
                    listOf(
                        generateTransaction("A1 Jan", Amount(BigDecimal("100")), true, janDate),
                        generateTransaction("A1 Feb", Amount(BigDecimal("100")), true, febDate)
                    )
                )

                getTrendStatsUseCase.handle(GetTrendStatsQuery(
                    userId = userId,
                    bookletId = booklet.id,
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = LocalDate.of(2025, 2, 28)
                )).assertTrue {
                    this.monthlyTrends.size == 2 && this.monthlyTrends.all { it.totalBooklets == 1 }
                }
            }
        }

        @Test
        fun `Should fail trend stats when period is partially provided`() {
            launchWithUserId {
                val result = getTrendStatsUseCase.handle(GetTrendStatsQuery(
                    userId = userId,
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = null
                ))

                result.assertFailure()
                assertEquals("domain.stats.trend.invalid_partial_date_range", result.errorInfo?.key)
            }
        }
    }

    @Nested
    inner class PrevisionalTransactionsTest {

        @Test
        fun `Should return previsional transactions within date range`() {
            launchWithUserId {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val transactions = listOf(
                    generateTransaction("Future Rent", Amount(BigDecimal("-800")), false, startDate.plusDays(15), isPreview = true),
                    generateTransaction("Future Salary", Amount(BigDecimal("2000")), true, startDate.plusMonths(1), isPreview = true),
                    generateTransaction("Future Bill", Amount(BigDecimal("-100")), false, startDate.plusMonths(2), isPreview = true)
                )
                initTransactions(transactions)

                getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(userId, startDate, endDate))
                    .assertTrue {
                        this.transactions.all {
                            it.date >= startDate && it.date <= endDate
                        }
                    }
            }
        }

        @Test
        fun `Should only include transactions marked as previsional`() {
            launchWithUserId {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val transactions = listOf(
                    generateTransaction("Future Rent", Amount(BigDecimal("-800")), false, startDate.plusDays(15), isPreview = true),
                    generateTransaction("Real Expense", Amount(BigDecimal("-100")), false, startDate.plusDays(10), isPreview = false),
                    generateTransaction("Future Salary", Amount(BigDecimal("2000")), true, startDate.plusMonths(1), isPreview = true)
                )
                initTransactions(transactions)

                getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(userId, startDate, endDate))
                    .assertTrue {
                        this.transactions.all { it.isPreview }
                    }
            }
        }

        @Test
        fun `Should group previsional transactions by booklet`() {
            launchWithUserId {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val transactions1 = listOf(
                    generateTransaction("Future Rent", Amount(BigDecimal("800")), false, startDate.plusDays(15), isPreview = true)
                )
                initTransactions(transactions1)

                val booklet2 = Booklet(Amount.fromString("500", "€".asCurrency()), "booklet2", owner = User(userId, "John", null), id = UUID.randomUUID())
                bookletState.init(listOf(BookletsByOwner(listOf(booklet2), userId)))
                transactionState.init(
                    listOf(IdBookletByTransaction(IdUserBooklet(userId, booklet2.id!!), mutableListOf(generateTransaction("Future Bill", Amount(BigDecimal("100")), false, startDate.plusMonths(1), isPreview = true))))
                )

                getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(userId, startDate, endDate))
                    .assertTrue {
                        this.groupedByBooklet.keys.size == 2
                    }
            }
        }

        @Test
        fun `Should calculate total previsional amount`() {
            launchWithUserId {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val transactions = listOf(
                    generateTransaction("Future Salary", Amount(BigDecimal("2000")), true, startDate.plusMonths(1), isPreview = true),
                    generateTransaction("Future Rent", Amount(BigDecimal("-800")), false, startDate.plusDays(15), isPreview = true)
                )
                initTransactions(transactions)

                getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(userId, startDate, endDate))
                    .assertTrue {
                        this.totalAmount.value != BigDecimal.ZERO
                    }
            }
        }

        @Test
        fun `Should fail when start date is after end date`() {
            launchWithUserId {
                val startDate = LocalDate.now().plusMonths(6)
                val endDate = LocalDate.now()

                val result = getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(userId, startDate, endDate))

                result.assertFailure()
                assertEquals("domain.stats.previsional.invalid_date_range", result.errorInfo?.key)
            }
        }

        @Test
        fun `Should return empty result when no previsional transactions exist`() {
            launchWithUserId {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                initTransactions(emptyList())

                getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(userId, startDate, endDate))
                    .assertTrue {
                        this.transactions.isEmpty()
                    }
            }
        }

        @Test
        fun `Should sort previsional transactions by date`() {
            launchWithUserId {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val transactions = listOf(
                    generateTransaction("Future 3", Amount(BigDecimal("-100")), false, startDate.plusMonths(2), isPreview = true),
                    generateTransaction("Future 1", Amount(BigDecimal("-800")), false, startDate.plusDays(15), isPreview = true),
                    generateTransaction("Future 2", Amount(BigDecimal("2000")), true, startDate.plusMonths(1), isPreview = true)
                )
                initTransactions(transactions)

                getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(userId, startDate, endDate))
                    .assertTrue {
                        val dates = this.transactions.map { it.date }
                        dates == dates.sorted()
                    }
            }
        }

        @Test
        fun `Should scope previsional transactions to selected booklet`() {
            launchWithUserId {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)

                initTransactions(
                    listOf(
                        generateTransaction("A1 Future", Amount(BigDecimal("-100")), false, startDate.plusDays(5), isPreview = true)
                    )
                )

                val secondBooklet = Booklet(
                    Amount.fromString("500", "€".asCurrency()),
                    "booklet2",
                    owner = User(userId, "John", null),
                    id = UUID.randomUUID()
                )
                bookletState.init(listOf(BookletsByOwner(listOf(secondBooklet), userId)))
                transactionState.init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(userId, secondBooklet.id!!),
                            mutableListOf(
                                generateTransaction("A2 Future", Amount(BigDecimal("-200")), false, startDate.plusDays(10), isPreview = true)
                            )
                        )
                    )
                )

                getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(userId, startDate, endDate, booklet.id))
                    .assertTrue {
                        this.groupedByBooklet.keys.size == 1
                    }
            }
        }

        @Test
        fun `Should split previsional transactions by regular and non regular`() {
            launchWithUserId {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(1)

                val regularPreview = generateTransaction(
                    "Loyer régulier",
                    Amount(BigDecimal("-800")),
                    false,
                    startDate.plusDays(5),
                    isPreview = true,
                ).copy(regularTransactionId = RegularTransactionId(UUID.randomUUID().toString()))

                val nonRegularPreview = generateTransaction(
                    "Facture ponctuelle",
                    Amount(BigDecimal("-120")),
                    false,
                    startDate.plusDays(8),
                    isPreview = true,
                )

                initTransactions(listOf(regularPreview, nonRegularPreview))

                getPrevisionalTransactionsUseCase.handle(GetPrevisionalTransactionsQuery(userId, startDate, endDate))
                    .assertTrue {
                        this.regularTransactions.size == 1 &&
                            this.nonRegularTransactions.size == 1
                    }
            }
        }
    }

    private fun generateTransactionWithTag(
        label: String,
        amount: Amount,
        date: LocalDate,
        tag: Tag
    ): Transaction {
        return Transaction(
            id = UUID.randomUUID(),
            label = label,
            date = date,
            amount = amount,
            isIncome = amount.value >= BigDecimal.ZERO,
            tag = tag,
            isPreview = false
        )
    }

    @Nested
    inner class DailyTrendStatsTest {

        @Test
        fun `Should return daily trends for a standard month`() {
            launchWithUserId {
                val transactions = listOf(
                    generateTransaction("Salary", Amount(BigDecimal("2000")), true, LocalDate.of(2025, 1, 5)),
                    generateTransaction("Groceries", Amount(BigDecimal("100")), false, LocalDate.of(2025, 1, 12))
                )
                initTransactions(transactions)

                getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                    userId = userId,
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = LocalDate.of(2025, 1, 31),
                    bookletId = booklet.id
                )).assertTrue {
                    this.dailyTrends.size == 31
                }
            }
        }

        @Test
        fun `Should return daily trends for cross-month custom cycle`() {
            launchWithUserId {
                val transactions = listOf(
                    generateTransaction("Income", Amount(BigDecimal("500")), true, LocalDate.of(2025, 5, 27)),
                    generateTransaction("Expense", Amount(BigDecimal("200")), false, LocalDate.of(2025, 6, 3))
                )
                initTransactions(transactions)

                getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                    userId = userId,
                    startDate = LocalDate.of(2025, 5, 25),
                    endDate = LocalDate.of(2025, 6, 24),
                    bookletId = booklet.id
                )).assertTrue {
                    this.dailyTrends.size == 31 &&
                        this.dailyTrends.first().date == LocalDate.of(2025, 5, 25) &&
                        this.dailyTrends.last().date == LocalDate.of(2025, 6, 24)
                }
            }
        }

        @Test
        fun `Should exclude preview transactions from daily trends`() {
            launchWithUserId {
                val transactions = listOf(
                    generateTransaction("Real", Amount(BigDecimal("100")), true, LocalDate.of(2025, 1, 5), isPreview = false),
                    generateTransaction("Preview", Amount(BigDecimal("999")), true, LocalDate.of(2025, 1, 5), isPreview = true)
                )
                initTransactions(transactions)

                getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                    userId = userId,
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = LocalDate.of(2025, 1, 31),
                    bookletId = booklet.id
                )).assertTrue {
                    val day5 = this.dailyTrends.find { it.date == LocalDate.of(2025, 1, 5) }
                    day5 != null && day5.income == Amount(BigDecimal("100"))
                }
            }
        }

        @Test
        fun `Should return zero entries when no transactions in range`() {
            launchWithUserId {
                initTransactions(emptyList())

                getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                    userId = userId,
                    startDate = LocalDate.of(2025, 3, 1),
                    endDate = LocalDate.of(2025, 3, 31),
                    bookletId = booklet.id
                )).assertTrue {
                    this.dailyTrends.size == 31 &&
                        this.dailyTrends.all { it.income.value.compareTo(BigDecimal.ZERO) == 0 }
                }
            }
        }

        @Test
        fun `Should fail when start date is after end date`() {
            launchWithUserId {
                val result = getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                    userId = userId,
                    startDate = LocalDate.of(2025, 6, 1),
                    endDate = LocalDate.of(2025, 5, 1),
                    bookletId = booklet.id
                ))

                result.assertFailure()
                assertEquals("domain.stats.daily_trend.invalid_date_range", result.errorInfo?.key)
            }
        }

        @Test
        fun `Should scope daily trends to selected booklet`() {
            launchWithUserId {
                val transactions = listOf(
                    generateTransaction("B1 Income", Amount(BigDecimal("300")), true, LocalDate.of(2025, 1, 5))
                )
                initTransactions(transactions)

                getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                    userId = userId,
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = LocalDate.of(2025, 1, 31),
                    bookletId = booklet.id
                )).assertTrue {
                    this.dailyTrends.all { it.totalBooklets == 1 }
                }
            }
        }

        @Test
        fun `Should compute cumulative balance correctly across days`() {
            launchWithUserId {
                val transactions = listOf(
                    generateTransaction("Income", Amount(BigDecimal("1000")), true, LocalDate.of(2025, 1, 1)),
                    generateTransaction("Expense", Amount(BigDecimal("300")), false, LocalDate.of(2025, 1, 10))
                )
                initTransactions(transactions)

                getDailyTrendStatsUseCase.handle(GetDailyTrendStatsQuery(
                    userId = userId,
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = LocalDate.of(2025, 1, 31),
                    bookletId = booklet.id
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
}
