package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.AuthenticationTest
import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.AccountByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserAccount
import fr.sacane.jmanager.domain.fake.IdUserAccountByTransaction
import fr.sacane.jmanager.domain.fake.UserTag
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
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
                val transactions = listOf(
                    generateTransaction("Salary", Amount(BigDecimal("2000")), true, LocalDate.of(2025, 1, 15)),
                    generateTransaction("Rent", Amount(BigDecimal("-800")), false, LocalDate.of(2025, 1, 5)),
                    generateTransaction("Groceries", Amount(BigDecimal("-200")), false, LocalDate.of(2025, 2, 10))
                )
                initTransactions(transactions)

                statsFeature.getMonthlyAccountStats(booklet.id!!, 2025, tokenValue)
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
                statsFeature.getMonthlyAccountStats(booklet.id!!, 2025, tokenValue)
                    .assertTrue {
                        this.monthlyData.all {
                            it.income.value.compareTo(BigDecimal.ZERO) == 0 &&
                                    it.expenses.value.compareTo(BigDecimal.ZERO) == 0
                        }
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
                val transactions = listOf(
                    generateTransaction("Future Salary", Amount(BigDecimal("2000")), true, LocalDate.of(2025, 1, 15), isPreview = true),
                    generateTransaction("Real Expense", Amount(BigDecimal("-100")), false, LocalDate.of(2025, 1, 10), isPreview = false)
                )
                initTransactions(transactions)

                statsFeature.getMonthlyAccountStats(booklet.id!!, 2025, tokenValue)
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
                val foodTag = Tag(id = 1L, label = "Food", isDefault = false)
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
                val foodTag = Tag(id = 1L, label = "Food", isDefault = false)
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
                val foodTag = Tag("Food",1L,  isDefault = false)
                val transportTag = Tag("Transport",2L, isDefault =  false)
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
                val transactions = listOf(
                    generateTransaction("Untagged expense", Amount(BigDecimal("-100")), false, LocalDate.of(2025, 1, 10))
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
                val foodTag = Tag(id = 1L, label = "Food", isDefault = false)
                val salaryTag = Tag(id = 2L, label = "Salary",  isDefault = false)

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
        fun `Should include all user accounts in trend calculation`() {
            launchWithConnectedUserInstance {
                val currentDate = LocalDate.now()
                val transactions1 = listOf(
                    generateTransaction("Transaction 1", Amount(BigDecimal("100")), true, currentDate)
                )
                initTransactions(transactions1)

                val booklet2 = Booklet(
                    Amount.fromString("500", "€".asCurrency()),
                    "account2",
                    owner = user.toUser(),
                    id = 51L
                )
                val transactions2 = listOf(
                    generateTransaction("Transaction 2", Amount(BigDecimal("200")), true, currentDate)
                )
                transactions2.forEach { booklet2.addTransaction(it) }

                accountState.init(listOf(AccountByOwner(listOf(booklet2), user.id)))

                statsFeature.getTrendStats(tokenValue)
                    .assertTrue {
                        this.monthlyTrends.all { it.totalAccounts == 2 }
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
        fun `Should group previsional transactions by account`() {
            launchWithConnectedUserInstance {
                val startDate = LocalDate.now()
                val endDate = startDate.plusMonths(3)
                val transactions1 = listOf(
                    generateTransaction("Future Rent", Amount(BigDecimal("800")), false, startDate.plusDays(15), isPreview = true)
                )
                initTransactions(transactions1)

                val booklet2 = Booklet(Amount.fromString("500", "€".asCurrency()), "account2", owner = user.toUser(), id = 51L)
                accountState.init(listOf(AccountByOwner(listOf(booklet2), user.id)))
                transactionState.init(
                    listOf(IdUserAccountByTransaction(IdUserAccount(user.id, booklet2.id!!), mutableListOf(generateTransaction("Future Bill", Amount(BigDecimal("100")), false, startDate.plusMonths(1), isPreview = true))))
                )

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

                statsFeature.getPrevisionalTransactions(tokenValue, startDate, endDate)
                    .assertFailure()
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
    }

    private fun generateTransactionWithTag(
        label: String,
        amount: Amount,
        date: LocalDate,
        tag: Tag
    ): Transaction {
        return Transaction(
            id = kotlin.random.Random.nextLong(),
            label = label,
            date = date,
            amount = amount,
            isIncome = amount.value >= BigDecimal.ZERO,
            tag = tag,
            isPreview = false
        )
    }
}