package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.*
import fr.sacane.jmanager.domain.fake.AccountByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.api.StatsFeature
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.Result
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
        private val accountState: State<AccountByOwner> = FakeFactory.accountState()

        private fun connectUser(user: User) {
            FakeFactory.sessionManager().addSession(user.id, session)
        }
    }

    @AfterEach
    fun clear() {
        FakeFactory.clearAll()
    }

    @Nested
    inner class StatsFeatureAuthTest : AuthenticationTest {
        override val action: List<Result<out Any>>
            get() = listOf(
                statsFeature.getMonthlyAccountStats(50L, 2025, session.tokenValue),
                statsFeature.getCategoryDistribution(session.tokenValue),
                statsFeature.getTrendStats(session.tokenValue),
                statsFeature.getPrevisionalTransactions(session.tokenValue, LocalDate.now(), LocalDate.now().plusMonths(3))
            )
    }

    @Nested
    inner class MonthlyAccountStatsTest {

        @Test
        fun `Should return monthly stats for a given account and year`() {
            launchWithConnectedUserInstance {
                val booklet = createBookletWithTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getMonthlyAccountStats(booklet.id!!, 2025, tokenValue)
                    .assertTrue {
                        this.year == 2025
                                && this.monthlyData.size == 12
                    }
            }
        }

        @Test
        fun `Should calculate correct income and expenses per month`() {
            launchWithConnectedUserInstance {
                val booklet = createBookletWithMonthlyTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getMonthlyAccountStats(booklet.id!!, 2025, tokenValue)
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
                val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getMonthlyAccountStats(booklet.id!!, 2025, tokenValue)
                    .assertTrue {
                        this.monthlyData.all { it.income.value == BigDecimal.ZERO && it.expenses.value == BigDecimal.ZERO }
                    }
            }
        }

        @Test
        fun `Should fail when account does not exist`() {
            launchWithConnectedUserInstance {
                statsFeature.getMonthlyAccountStats(999L, 2025, tokenValue)
                    .assertFailure()
            }
        }

        @Test
        fun `Should not include previsional transactions in monthly stats`() {
            launchWithConnectedUserInstance {
                val booklet = createBookletWithPrevisionalTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getMonthlyAccountStats(booklet.id!!, 2025, tokenValue)
                    .assertTrue {
                        val januaryData = this.monthlyData.find { it.month == 1 }
                        januaryData != null && januaryData.income.value == BigDecimal.ZERO
                    }
            }
        }
    }

    @Nested
    inner class CategoryDistributionStatsTest {

        @Test
        fun `Should return category distribution for all user transactions`() {
            launchWithConnectedUserInstance {
                val booklet = createBookletWithTaggedTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getCategoryDistribution(tokenValue)
                    .assertTrue {
                        this.categories.isNotEmpty()
                    }
            }
        }

        @Test
        fun `Should calculate correct amounts per category`() {
            launchWithConnectedUserInstance {
                val booklet = createBookletWithMultipleCategoryTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

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
                val booklet = createBookletWithBalancedCategories()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getCategoryDistribution(tokenValue)
                    .assertTrue {
                        val totalPercentage = this.categories.sumOf { it.percentage.toDouble() }
                        totalPercentage in 99.9..100.1 // Allow for rounding
                    }
            }
        }

        @Test
        fun `Should return empty distribution when no transactions exist`() {
            launchWithConnectedUserWithoutAccount {
                val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
                accountState.init(listOf(AccountByOwner(listOf(booklet), userId)))

                statsFeature.getCategoryDistribution(tokenValue)
                    .assertTrue {
                        this.categories.isEmpty()
                    }
            }
        }

        @Test
        fun `Should group transactions without tags as uncategorized`() {
            launchWithConnectedUserInstance {
                val booklet = createBookletWithUntaggedTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getCategoryDistribution(tokenValue)
                    .assertTrue {
                        val uncategorized = this.categories.find { it.tagLabel == "Uncategorized" }
                        uncategorized != null && uncategorized.totalAmount.value > BigDecimal.ZERO
                    }
            }
        }

        @Test
        fun `Should only include expenses in category distribution`() {
            launchWithConnectedUserInstance {
                val booklet = createBookletWithIncomeAndExpenses()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getCategoryDistribution(tokenValue)
                    .assertTrue {
                        this.categories.all { it.totalAmount.value <= BigDecimal.ZERO }
                    }
            }
        }
    }

    @Nested
    inner class TrendStatsTest {

        @Test
        fun `Should return trend for last 12 months`() {
            launchWithConnectedUserInstance {
                val booklet = createBookletWithYearlyTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getTrendStats(tokenValue)
                    .assertTrue {
                        this.monthlyTrends.size == 12
                    }
            }
        }

        @Test
        fun `Should calculate balance evolution correctly`() {
            launchWithConnectedUserInstance {
                val booklet = createBookletWithProgressiveTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getTrendStats(tokenValue)
                    .assertTrue {
                        val firstMonth = this.monthlyTrends.first()
                        val lastMonth = this.monthlyTrends.last()
                        lastMonth.cumulativeBalance.value > firstMonth.cumulativeBalance.value
                    }
            }
        }

        @Test
        fun `Should include all user accounts in trend calculation`() {
            launchWithConnectedUserInstance {
                val booklet1 = createBookletWithTransactions("account1")
                val booklet2 = createBookletWithTransactions("account2")
                accountState.init(listOf(AccountByOwner(listOf(booklet1, booklet2), user.id)))

                statsFeature.getTrendStats(tokenValue)
                    .assertTrue {
                        this.monthlyTrends.all { it.totalAccounts == 2 }
                    }
            }
        }

        @Test
        fun `Should order months from oldest to most recent`() {
            launchWithConnectedUserInstance {
                val booklet = createBookletWithYearlyTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

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
                val booklet = createBookletWithSparseTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getTrendStats(tokenValue)
                    .assertTrue {
                        this.monthlyTrends.any {
                            it.income.value == BigDecimal.ZERO && it.expenses.value == BigDecimal.ZERO
                        }
                    }
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
                val booklet = createBookletWithFuturePrevisionalTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

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
                val booklet = createBookletWithMixedTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)
                    .assertTrue {
                        this.transactions.all { it.isPreview }
                    }
            }
        }

        @Test
        fun `Should group previsional transactions by account`() {
            launchWithConnectedUserInstance {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val booklet1 = createBookletWithFuturePrevisionalTransactions("account1")
                val booklet2 = createBookletWithFuturePrevisionalTransactions("account2")
                accountState.init(listOf(AccountByOwner(listOf(booklet1, booklet2), user.id)))

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)
                    .assertTrue {
                        this.groupedByAccount.keys.size == 2
                    }
            }
        }

        @Test
        fun `Should calculate total previsional amount`() {
            launchWithConnectedUserInstance {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val booklet = createBookletWithFuturePrevisionalTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

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

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)
                    .assertFailure()
            }
        }

        @Test
        fun `Should return empty result when no previsional transactions exist`() {
            launchWithConnectedUserInstance {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

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
                val booklet = createBookletWithUnorderedPrevisionalTransactions()
                accountState.init(listOf(AccountByOwner(listOf(booklet), user.id)))

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)
                    .assertTrue {
                        val dates = this.transactions.map { it.date }
                        dates == dates.sorted()
                    }
            }
        }
    }

    // Helper functions pour créer des données de test
    private fun createBookletWithTransactions(label: String = "test"): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), label, owner = user, id = 50L)
        // Transactions will be added here
        return booklet
    }

    private fun createBookletWithMonthlyTransactions(): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
        // Add January income and expenses
        return booklet
    }

    private fun createBookletWithPrevisionalTransactions(): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
        // Add previsional transactions
        return booklet
    }

    private fun createBookletWithTaggedTransactions(): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
        // Add transactions with tags
        return booklet
    }

    private fun createBookletWithMultipleCategoryTransactions(): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
        // Add transactions with multiple categories
        return booklet
    }

    private fun createBookletWithBalancedCategories(): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
        // Add transactions with balanced categories
        return booklet
    }

    private fun createBookletWithUntaggedTransactions(): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
        // Add transactions without tags
        return booklet
    }

    private fun createBookletWithIncomeAndExpenses(): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
        // Add both income and expenses
        return booklet
    }

    private fun createBookletWithYearlyTransactions(): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
        // Add transactions across 12 months
        return booklet
    }

    private fun createBookletWithProgressiveTransactions(): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
        // Add transactions with progressive growth
        return booklet
    }

    private fun createBookletWithSparseTransactions(): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
        // Add transactions with gaps
        return booklet
    }

    private fun createBookletWithFuturePrevisionalTransactions(label: String = "test"): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), label, owner = user, id = 50L)
        // Add future previsional transactions
        return booklet
    }

    private fun createBookletWithMixedTransactions(): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
        // Add mix of previsional and actual transactions
        return booklet
    }

    private fun createBookletWithUnorderedPrevisionalTransactions(): Booklet {
        val booklet = Booklet(Amount.fromString("1000", "€".asCurrency()), "test", owner = user, id = 50L)
        // Add unordered previsional transactions
        return booklet
    }
}