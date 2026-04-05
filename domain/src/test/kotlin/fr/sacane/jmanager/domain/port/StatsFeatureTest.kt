package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.AuthenticationTest
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
import fr.sacane.jmanager.domain.port.api.StatsFeature
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.Result
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class StatsFeatureTest : FeatureTest() {

    companion object {
        private val userRepository: UserRepository = FakeFactory.fakeUserRepository()
        private var statsFeature: StatsFeature = FakeFactory.statsFeature
        private val user = userRepository.register("jojo", "test") as User
        private val tokenValue = "${user.id.value}||${UUID.randomUUID()}||${Role.USER.name}||${user.username}"
        private val session: AccessToken = AccessToken(userId = user.id, user.username, tokenValue)
        private val bookletState: State<BookletsByOwner> = FakeFactory.bookletState()
        private val transactionState = FakeFactory.fakeTransactionRepository()

    }

    @AfterEach
    fun clear() {
        FakeFactory.clearAll()
    }

    @Nested
    inner class StatsFeatureAuthTest : AuthenticationTest {
        override val action: List<Result<out Any>>
            get() = listOf(
                statsFeature.getMonthlyBookletStats(UUID.randomUUID(), 2025, session.tokenValue),
                statsFeature.getCategoryDistribution(session.tokenValue),
                statsFeature.getTrendStats(session.tokenValue),
                statsFeature.getPrevisionalTransactions(session.tokenValue, LocalDate.now(), LocalDate.now().plusMonths(3))
            )
    }

    @Nested
    inner class MonthlyBookletStatsTest {

        @Test
        fun `Should return monthly stats for a given booklet and year`() {
            launchWithConnectedUserInstance {
                val transactions = listOf(
                    generateTransaction("Salary", Amount(BigDecimal("2000")), true, LocalDate.of(2025, 1, 15)),
                    generateTransaction("Rent", Amount(BigDecimal("-800")), false, LocalDate.of(2025, 1, 5)),
                    generateTransaction("Groceries", Amount(BigDecimal("-200")), false, LocalDate.of(2025, 2, 10))
                )
                initTransactions(transactions)

                statsFeature.getMonthlyBookletStats(booklet.id!!, 2025, tokenValue)
                    .assertTrue {
                        this.year == 2025 && this.monthlyData.size == 12
                    }
            }
        }

        @Test
        fun `Should calculate correct income and expenses per month`() {
            launchWithConnectedUserInstance {
                val transactions = listOf(
                    generateTransaction("Salary", Amount(BigDecimal("500")), true, LocalDate.of(2025, 1, 15)),
                    generateTransaction("Groceries", Amount(BigDecimal("-200")), false, LocalDate.of(2025, 1, 10))
                )
                initTransactions(transactions)

                statsFeature.getMonthlyBookletStats(booklet.id!!, 2025, tokenValue)
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
            launchWithConnectedUserInstance {
                statsFeature.getMonthlyBookletStats(booklet.id!!, 2025, tokenValue)
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
            launchWithConnectedUserInstance {
                val result = statsFeature.getMonthlyBookletStats(UUID.randomUUID(), 2025, tokenValue)

                result.assertFailure()
                assertEquals("domain.stats.monthly.booklet_not_found", result.errorInfo?.key)
            }
        }

        @Test
        fun `Should not include previsional transactions in monthly stats`() {
            launchWithConnectedUserInstance {
                val transactions = listOf(
                    generateTransaction("Future Salary", Amount(BigDecimal("2000")), true, LocalDate.of(2025, 1, 15), isPreview = true),
                    generateTransaction("Real Expense", Amount(BigDecimal("-100")), false, LocalDate.of(2025, 1, 10), isPreview = false)
                )
                initTransactions(transactions)

                statsFeature.getMonthlyBookletStats(booklet.id!!, 2025, tokenValue)
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
            launchWithConnectedUserInstance {
                val foodTag = Tag(id = UUID.randomUUID(), label = "Food", isDefault = false)
                initTags(listOf(UserTag(user.id, mutableListOf(foodTag))))
                val transactions = listOf(
                    generateTransactionWithTag("Groceries", Amount(BigDecimal("-100")), LocalDate.of(2025, 1, 10), foodTag),
                    generateTransactionWithTag("Restaurant", Amount(BigDecimal("-50")), LocalDate.of(2025, 1, 15), foodTag)
                )
                initTransactions(transactions)

                statsFeature.getCategoryDistribution(tokenValue)
                    .assertTrue {
                        this.categories.isNotEmpty()
                    }
            }
        }

        @Test
        fun `Should calculate correct amounts per category`() {
            launchWithConnectedUserInstance {
                val foodTag = Tag(id = UUID.randomUUID(), label = "Food", isDefault = false)
                initTags(listOf(UserTag(user.id, mutableListOf(foodTag))))

                val transactions = listOf(
                    generateTransactionWithTag("Groceries", Amount(BigDecimal("-100")), LocalDate.of(2025, 1, 10), foodTag),
                    generateTransactionWithTag("Restaurant", Amount(BigDecimal("-200")), LocalDate.of(2025, 1, 15), foodTag)
                )
                initTransactions(transactions)

                statsFeature.getCategoryDistribution(tokenValue)
                    .assertTrue {
                        val foodCategory = this.categories.find { it.tagLabel == "Food" }
                        foodCategory != null && foodCategory.totalAmount == Amount(BigDecimal("300"))
                    }
            }
        }

        @Test
        fun `Should calculate correct percentages for each category`() {
            launchWithConnectedUserInstance {
                val foodTag = Tag("Food", UUID.randomUUID(),  isDefault = false)
                val transportTag = Tag("Transport",UUID.randomUUID(), isDefault =  false)
                initTags(listOf(UserTag(user.id, mutableListOf(foodTag, transportTag))))

                val transactions = listOf(
                    generateTransactionWithTag("Groceries", Amount(BigDecimal("-500")), LocalDate.of(2025, 1, 10), foodTag),
                    generateTransactionWithTag("Gas", Amount(BigDecimal("-500")), LocalDate.of(2025, 1, 15), transportTag)
                )
                initTransactions(transactions)

                statsFeature.getCategoryDistribution(tokenValue)
                    .assertTrue {
                        val totalPercentage = this.categories.sumOf { it.percentage.toDouble() }
                        totalPercentage in 99.9..100.1
                    }
            }
        }

        @Test
        fun `Should return empty distribution when no transactions exist`() {
            launchWithConnectedUserWithoutBooklet {
                val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = UUID.randomUUID())
                bookletState.init(listOf(BookletsByOwner(listOf(booklet), userId)))

                statsFeature.getCategoryDistribution(tokenValue)
                    .assertTrue {
                        this.categories.isEmpty()
                    }
            }
        }

        @Test
        fun `Should group transactions without tags as uncategorized`() {
            launchWithConnectedUserInstance {
                val transactions = listOf(
                    generateTransaction("Untagged expense", Amount(BigDecimal("100")), false, LocalDate.of(2025, 1, 10))
                )
                initTransactions(transactions)

                val result = statsFeature.getCategoryDistribution(tokenValue)

                result.assertTrue {
                    val uncategorized = this.categories.find { it.tagLabel == "Aucune" }
                    uncategorized != null && uncategorized.totalAmount.value > BigDecimal.ZERO
                }
            }
        }

        @Test
        fun `Should only include expenses in category distribution`() {
            launchWithConnectedUserInstance {
                val foodTagId = UUID.randomUUID()
                val salaryTagId = UUID.randomUUID()
                val foodTag = Tag(id = foodTagId, label = "Food", isDefault = false)
                val salaryTag = Tag(id = salaryTagId, label = "Salary",  isDefault = false)

                initTags(listOf(UserTag(user.id, mutableListOf(foodTag, salaryTag))))

                val transactions = listOf(
                    generateTransactionWithTag("Groceries", Amount(BigDecimal("-100")), LocalDate.of(2025, 1, 10), foodTag),
                    generateTransactionWithTag("Monthly Salary", Amount(BigDecimal("2000")), LocalDate.of(2025, 1, 15), salaryTag)
                )
                initTransactions(transactions)

                statsFeature.getCategoryDistribution(tokenValue)
                    .assertTrue {
                        this.categories.all { it.totalAmount.value >= BigDecimal.ZERO }
                    }
            }
        }

        @Test
        fun `Should scope category distribution to selected booklet`() {
            launchWithConnectedUserInstance {
                val foodTag = Tag(id = UUID.randomUUID(), label = "Food", isDefault = false)
                initTags(listOf(UserTag(user.id, mutableListOf(foodTag))))

                val secondBooklet = Booklet(
                    Amount.fromString("500", "€".asCurrency()),
                    "booklet2",
                    owner = user.toUser(),
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
                bookletState.init(listOf(BookletsByOwner(listOf(secondBooklet), user.id)))
                transactionState.init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(user.id, secondBooklet.id!!),
                            mutableListOf(secondBookletTransaction)
                        )
                    )
                )

                statsFeature.getCategoryDistribution(tokenValue, bookletId = booklet.id)
                    .assertTrue {
                        this.totalExpenses == Amount(BigDecimal("100"))
                    }
            }
        }

        @Test
        fun `Should fail category distribution when period is partially provided`() {
            launchWithConnectedUserInstance {
                val result = statsFeature.getCategoryDistribution(
                    token = tokenValue,
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = null
                )

                result.assertFailure()
                assertEquals("domain.stats.category_distribution.invalid_partial_date_range", result.errorInfo?.key)
            }
        }
    }

    @Nested
    inner class TrendStatsTest {

        @Test
        fun `Should return trend for last 12 months`() {
            launchWithConnectedUserInstance {
                val currentDate = LocalDate.now()
                val transactions = (0..11).map { monthsAgo ->
                    val date = currentDate.minusMonths(monthsAgo.toLong())
                    generateTransaction("Transaction $monthsAgo", Amount(BigDecimal("100")), true, date)
                }
                initTransactions(transactions)

                statsFeature.getTrendStats(tokenValue)
                    .assertTrue {
                        this.monthlyTrends.size == 12
                    }
            }
        }

        @Test
        fun `Should calculate balance evolution correctly`() {
            launchWithConnectedUserInstance {
                val currentDate = LocalDate.now()
                val transactions = (0..11).map { monthsAgo ->
                    val date = currentDate.minusMonths(monthsAgo.toLong())
                    generateTransaction("Income $monthsAgo", Amount(BigDecimal("${100 * (monthsAgo + 1)}")), true, date)
                }
                initTransactions(transactions)

                statsFeature.getTrendStats(tokenValue)
                    .assertTrue {
                        val firstMonth = this.monthlyTrends.first()
                        val lastMonth = this.monthlyTrends.last()
                        lastMonth.cumulativeBalance.value > firstMonth.cumulativeBalance.value
                    }
            }
        }

        @Test
        fun `Should include all user booklets in trend calculation`() {
            launchWithConnectedUserInstance {
                val currentDate = LocalDate.now()
                val transactions1 = listOf(
                    generateTransaction("Transaction 1", Amount(BigDecimal("100")), true, currentDate)
                )
                initTransactions(transactions1)

                val booklet2 = Booklet(
                    Amount.fromString("500", "€".asCurrency()),
                    "booklet2",
                    owner = user.toUser(),
                    id = UUID.randomUUID()
                )
                val transactions2 = listOf(
                    generateTransaction("Transaction 2", Amount(BigDecimal("200")), true, currentDate)
                )
                transactions2.forEach { booklet2.addTransaction(it) }

                bookletState.init(listOf(BookletsByOwner(listOf(booklet2), user.id)))

                statsFeature.getTrendStats(tokenValue)
                    .assertTrue {
                        this.monthlyTrends.all { it.totalBooklets == 2 }
                    }
            }
        }

        @Test
        fun `Should order months from oldest to most recent`() {
            launchWithConnectedUserInstance {
                val currentDate = LocalDate.now()
                val transactions = (0..11).map { monthsAgo ->
                    val date = currentDate.minusMonths(monthsAgo.toLong())
                    generateTransaction("Transaction $monthsAgo", Amount(BigDecimal("100")), true, date)
                }
                initTransactions(transactions)

                statsFeature.getTrendStats(tokenValue)
                    .assertTrue {
                        val months = this.monthlyTrends.map { it.month to it.year }
                        months == months.sortedWith(compareBy({ it.second }, { it.first }))
                    }
            }
        }

        @Test
        fun `Should handle months with no transactions`() {
            launchWithConnectedUserInstance {
                val currentDate = LocalDate.now()
                val transactions = listOf(
                    generateTransaction("Transaction 1", Amount(BigDecimal("100")), true, currentDate.minusMonths(10)),
                    generateTransaction("Transaction 2", Amount(BigDecimal("100")), true, currentDate.minusMonths(5))
                )
                initTransactions(transactions)

                statsFeature.getTrendStats(tokenValue)
                    .assertTrue {
                        this.monthlyTrends.any {
                            it.income.value.compareTo(BigDecimal.ZERO) == 0 && it.expenses.value.compareTo(BigDecimal.ZERO) == 0
                        }
                    }
            }
        }

        @Test
        fun `Should scope trend stats to selected booklet and period`() {
            launchWithConnectedUserInstance {
                val janDate = LocalDate.of(2025, 1, 5)
                val febDate = LocalDate.of(2025, 2, 5)
                initTransactions(
                    listOf(
                        generateTransaction("A1 Jan", Amount(BigDecimal("100")), true, janDate),
                        generateTransaction("A1 Feb", Amount(BigDecimal("100")), true, febDate)
                    )
                )

                statsFeature.getTrendStats(
                    token = tokenValue,
                    bookletId = booklet.id,
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = LocalDate.of(2025, 2, 28)
                ).assertTrue {
                    this.monthlyTrends.size == 2 && this.monthlyTrends.all { it.totalBooklets == 1 }
                }
            }
        }

        @Test
        fun `Should fail trend stats when period is partially provided`() {
            launchWithConnectedUserInstance {
                val result = statsFeature.getTrendStats(
                    token = tokenValue,
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = null
                )

                result.assertFailure()
                assertEquals("domain.stats.trend.invalid_partial_date_range", result.errorInfo?.key)
            }
        }
    }

    @Nested
    inner class PrevisionalTransactionsTest {

        @Test
        fun `Should return previsional transactions within date range`() {
            launchWithConnectedUserInstance {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val transactions = listOf(
                    generateTransaction("Future Rent", Amount(BigDecimal("-800")), false, startDate.plusDays(15), isPreview = true),
                    generateTransaction("Future Salary", Amount(BigDecimal("2000")), true, startDate.plusMonths(1), isPreview = true),
                    generateTransaction("Future Bill", Amount(BigDecimal("-100")), false, startDate.plusMonths(2), isPreview = true)
                )
                initTransactions(transactions)

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)
                    .assertTrue {
                        this.transactions.all {
                            it.date >= startDate && it.date <= endDate
                        }
                    }
            }
        }

        @Test
        fun `Should only include transactions marked as previsional`() {
            launchWithConnectedUserInstance {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val transactions = listOf(
                    generateTransaction("Future Rent", Amount(BigDecimal("-800")), false, startDate.plusDays(15), isPreview = true),
                    generateTransaction("Real Expense", Amount(BigDecimal("-100")), false, startDate.plusDays(10), isPreview = false),
                    generateTransaction("Future Salary", Amount(BigDecimal("2000")), true, startDate.plusMonths(1), isPreview = true)
                )
                initTransactions(transactions)

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)
                    .assertTrue {
                        this.transactions.all { it.isPreview }
                    }
            }
        }

        @Test
        fun `Should group previsional transactions by booklet`() {
            launchWithConnectedUserInstance {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val transactions1 = listOf(
                    generateTransaction("Future Rent", Amount(BigDecimal("800")), false, startDate.plusDays(15), isPreview = true)
                )
                initTransactions(transactions1)

                val booklet2 = Booklet(Amount.fromString("500", "€".asCurrency()), "booklet2", owner = user.toUser(), id = UUID.randomUUID())
                bookletState.init(listOf(BookletsByOwner(listOf(booklet2), user.id)))
                transactionState.init(
                    listOf(IdBookletByTransaction(IdUserBooklet(user.id, booklet2.id!!), mutableListOf(generateTransaction("Future Bill", Amount(BigDecimal("100")), false, startDate.plusMonths(1), isPreview = true))))
                )

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)
                    .assertTrue {
                        this.groupedByBooklet.keys.size == 2
                    }
            }
        }

        @Test
        fun `Should calculate total previsional amount`() {
            launchWithConnectedUserInstance {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val transactions = listOf(
                    generateTransaction("Future Salary", Amount(BigDecimal("2000")), true, startDate.plusMonths(1), isPreview = true),
                    generateTransaction("Future Rent", Amount(BigDecimal("-800")), false, startDate.plusDays(15), isPreview = true)
                )
                initTransactions(transactions)

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)
                    .assertTrue {
                        this.totalAmount.value != BigDecimal.ZERO
                    }
            }
        }

        @Test
        fun `Should fail when start date is after end date`() {
            launchWithConnectedUserInstance {
                val startDate = LocalDate.now().plusMonths(6)
                val endDate = LocalDate.now()

                val result = statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)

                result.assertFailure()
                assertEquals("domain.stats.previsional.invalid_date_range", result.errorInfo?.key)
            }
        }

        @Test
        fun `Should return empty result when no previsional transactions exist`() {
            launchWithConnectedUserInstance {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                initTransactions(emptyList())

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)
                    .assertTrue {
                        this.transactions.isEmpty()
                    }
            }
        }

        @Test
        fun `Should sort previsional transactions by date`() {
            launchWithConnectedUserInstance {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val transactions = listOf(
                    generateTransaction("Future 3", Amount(BigDecimal("-100")), false, startDate.plusMonths(2), isPreview = true),
                    generateTransaction("Future 1", Amount(BigDecimal("-800")), false, startDate.plusDays(15), isPreview = true),
                    generateTransaction("Future 2", Amount(BigDecimal("2000")), true, startDate.plusMonths(1), isPreview = true)
                )
                initTransactions(transactions)

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)
                    .assertTrue {
                        val dates = this.transactions.map { it.date }
                        dates == dates.sorted()
                    }
            }
        }

        @Test
        fun `Should scope previsional transactions to selected booklet`() {
            launchWithConnectedUserInstance {
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
                    owner = user.toUser(),
                    id = UUID.randomUUID()
                )
                bookletState.init(listOf(BookletsByOwner(listOf(secondBooklet), user.id)))
                transactionState.init(
                    listOf(
                        IdBookletByTransaction(
                            IdUserBooklet(user.id, secondBooklet.id!!),
                            mutableListOf(
                                generateTransaction("A2 Future", Amount(BigDecimal("-200")), false, startDate.plusDays(10), isPreview = true)
                            )
                        )
                    )
                )

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate, booklet.id)
                    .assertTrue {
                        this.groupedByBooklet.keys.size == 1
                    }
            }
        }

        @Test
        fun `Should split previsional transactions by regular and non regular`() {
            launchWithConnectedUserInstance {
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

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)
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
}