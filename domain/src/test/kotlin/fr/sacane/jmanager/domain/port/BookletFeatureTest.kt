package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.*
import fr.sacane.jmanager.domain.fake.BookletsByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.IdUserBooklet
import fr.sacane.jmanager.domain.fake.IdBookletByTransaction
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fake.UserRegularTransaction
import fr.sacane.jmanager.domain.fixture.BookletFixture
import fr.sacane.jmanager.domain.fixture.UserFixture
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.TransactionSortDirection
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.input.booklet.*
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class BookletFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val userRepository = factory.fakeUserRepository()
    private val findBookletByIdService = factory.findBookletByIdService
    private val editBookletService = factory.editBookletService
    private val deleteBookletByIdService = factory.deleteBookletByIdService
    private val findByLabelAndUserIdService = factory.findByLabelAndUserIdService
    private val findAllRegisteredBookletsService = factory.findAllRegisteredBookletsService
    private val saveBookletService = factory.saveBookletService
    private val loadTransactionsForBookletForAMonthService = factory.loadTransactionsForBookletForAMonthService
    private val loadBalancesForBookletForAMonthService = factory.loadBalancesForBookletForAMonthService
    private val regenerateDeletedPrevisionalTransactionsService = factory.regenerateDeletedPrevisionalTransactionsService
    private val bookletState: State<BookletsByOwner> = factory.bookletState()

    @AfterEach
    fun clear() {
        factory.clearAll()
    }

    @Test
    fun `Should find booklet by its Id`() {
        val id = UUID.randomUUID()
        val ctx = scenario.withUser()
        val element = BookletFixture.aBooklet(id = id, label = "test", amount = Amount(100), owner = ctx.user)
        factory.bookletState().initWith(BookletsByOwner(listOf(element), ctx.userId))
        findBookletByIdService.handle(FindBookletByIdQuery(id, ctx.userId))
            .assertTrue { this.label == "test" }
    }

    @Test
    fun `Given an existing booklet it could be edit`() {
        val ctx = scenario.withUser()
        val element = BookletFixture.aBooklet(label = "test", amount = Amount(100), owner = ctx.user)
        factory.bookletState().initWith(BookletsByOwner(listOf(element), ctx.userId))
        val updatedBooklet = Booklet(
            Amount(BigDecimal(102)),
            label = element.label,
            initialSold = element.initialSold,
            owner = ctx.user,
            id = element.id,
        )
        val response = act { editBookletService.handle(EditBookletCommand(updatedBooklet, ctx.userId)) }
        then(response) { map { it.amount }.assertEquals(Amount(BigDecimal(102))) }
    }

    @Test
    fun `As an owner of an booklet, I can delete it`() {
        val ctx = scenario.withUser()
        val element = BookletFixture.aBooklet(label = "test", amount = Amount(100), owner = ctx.user)
        factory.bookletState().initWith(BookletsByOwner(listOf(element), ctx.userId))

        deleteBookletByIdService.handle(DeleteBookletByIdCommand(element.id!!, ctx.userId)).assertTrue {
            val bookletsList = bookletState.getStates()
            val expectedBookletSize = 0
            val actualBookletSize = bookletsList.find { it.userId == ctx.userId }?.booklets?.size ?: throw Error()
            expectedBookletSize == actualBookletSize
        }

        val bookletsList = bookletState.getStates()
        val ofUser = bookletsList.find { it.userId == ctx.userId }!!
        assertNull(ofUser.existsById(UUID.randomUUID()))
    }

    @Test
    fun `As an booklet's owner, I can retrieve it by its label`() {
        val ctx = scenario.withUser()
        val element = BookletFixture.aBooklet(label = "test22", amount = Amount(100), owner = ctx.user)
        factory.bookletState().initWith(BookletsByOwner(listOf(element), ctx.userId))
        findByLabelAndUserIdService.handle(FindByLabelAndUserIdQuery(ctx.userId, element.label))
            .assertTrue { this.label == "test22" && this.amount == Amount(100) }
    }

    @Test
    fun `As a booklet's owner,  I can retrieve All of my Registered Booklets`() {
        val ctx = scenario.withUser()
        val booklet = BookletFixture.aBooklet(label = "test1", amount = Amount(100), owner = ctx.user)
        val booklet2 = BookletFixture.aBooklet(label = "test2", amount = Amount(100), owner = ctx.user)
        val booklet3 = BookletFixture.aBooklet(label = "test3", amount = Amount(100), owner = ctx.user)
        val booklet4 = BookletFixture.aBooklet(label = "test4", amount = Amount(100), owner = ctx.user)
        val expectedBookletList = listOf(booklet, booklet2, booklet3, booklet4)
        factory.bookletState().initWith(BookletsByOwner(expectedBookletList, ctx.userId))
        findAllRegisteredBookletsService.handle(FindAllRegisteredBookletsQuery(ctx.userId))
            .assertContainsAtPosition(0, booklet)
            .assertContainsAtPosition(1, booklet2)
            .assertContainsAtPosition(2, booklet3)
            .assertContainsAtPosition(3, booklet4)
    }

    @Test
    fun `As a Jmanager user, I can create new booklet`() {
        val ctx = scenario.withUser()
        val bookletToSave = BookletFixture.aBooklet(label = "test1", amount = Amount(100), owner = ctx.user)
        saveBookletService.handle(SaveBookletCommand(ctx.userId, bookletToSave))
            .assertTrue { this.amount == Amount(100) && this.label == "test1" }
    }

    @Test
    fun `As a simple user, I cannot create more than six booklets`() {
        val ctx = scenario.withUser()
        val bookletLists = (0 until 6).map { BookletFixture.aBooklet(label = "test$it", amount = Amount(100), owner = ctx.user) }
        factory.bookletState().initWith(BookletsByOwner(bookletLists, ctx.userId))
        val extraBooklet = BookletFixture.aBooklet(label = "test7", amount = Amount(100), owner = ctx.user)
        val result = act { saveBookletService.handle(SaveBookletCommand(ctx.userId, extraBooklet)) }
        then(result) {
            assertFailure(ResultState.BOOKLET_MAXIMUM_SIZE_REACHED)
            assertEquals("domain.booklet.save.maximum_size_reached", errorInfo?.key)
        }
    }

    @Test
    fun `As a booklet's owner, I cannot register an existing booklet with the same label`() {
        val testUser = userRepository.register("testBookletOwner", "pass") as User
        val existingBooklet = BookletFixture.aBooklet(label = "test1", amount = Amount(100), owner = testUser)
        factory.bookletState().initWith(BookletsByOwner(listOf(existingBooklet), testUser.id))
        val bookletToSave = BookletFixture.aBooklet(label = "test1", amount = Amount(150), owner = testUser)
        saveBookletService.handle(SaveBookletCommand(testUser.id, bookletToSave))
            .assertFailure(ResultState.BOOKLET_LABEL_EXIST)
    }

    @Test
    fun `Deleting a booklet owned by another user should be forbidden`() {
        val attackerCtx = scenario.withUser()
        val victimUser = userRepository.register("victim", "pass") as User
        val victimBooklet = BookletFixture.aBooklet(label = "victim-booklet", amount = Amount(500), owner = victimUser)
        factory.bookletState().initWith(BookletsByOwner(listOf(victimBooklet), victimUser.id))
        deleteBookletByIdService.handle(DeleteBookletByIdCommand(victimBooklet.id!!, attackerCtx.userId))
            .assertFailure(ResultState.FORBIDDEN)
    }

    @Test
    fun `Editing a booklet owned by another user should be forbidden`() {
        val attackerCtx = scenario.withUser()
        val victimUser = userRepository.register("victim2", "pass") as User
        val victimBooklet = BookletFixture.aBooklet(label = "victim-booklet2", amount = Amount(500), owner = victimUser)
        factory.bookletState().initWith(BookletsByOwner(listOf(victimBooklet), victimUser.id))
        val editedBooklet = BookletFixture.aBooklet(label = "hacked", amount = Amount(999), owner = victimUser, id = victimBooklet.id)
        editBookletService.handle(EditBookletCommand(editedBooklet, attackerCtx.userId))
            .assertFailure(ResultState.FORBIDDEN)
    }

    @Nested
    inner class LoadTransactionsForBookletForAMonthTest {

        @Test
        fun `Should calculate real sold correctly with income transactions only`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Test Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transaction1 = Transaction(
                id = UUID.randomUUID(), label = "Salary", date = LocalDate.of(2025, 1, 15),
                amount = 2000.toAmount(), isIncome = true, isPreview = false
            )
            val transaction2 = Transaction(
                id = UUID.randomUUID(), label = "Bonus", date = LocalDate.of(2025, 1, 20),
                amount = 500.toAmount(), isIncome = true, isPreview = false
            )
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(transaction1, transaction2))
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025
            ))
            result.assertTrue { this.realSold == 3500.toAmount() } // 1000 + 2000 + 500
            result.assertTrue { this.currentTransactions.size == 2 }
            result.assertTrue { this.currentTransactions.all { it.isIncome } }
        }

        @Test
        fun `Should calculate real sold correctly with expense transactions only`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Test Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transaction1 = Transaction(
                id = UUID.randomUUID(),
                label = "Rent",
                date = LocalDate.of(2025, 1, 5),
                amount = 500.toAmount(),
                isIncome = false,
                isPreview = false
            )
            val transaction2 = Transaction(
                id = UUID.randomUUID(),
                label = "Groceries",
                date = LocalDate.of(2025, 1, 10),
                amount = 200.toAmount(),
                isIncome = false,
                isPreview = false
            )
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(transaction1, transaction2))
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025
            ))
            result.assertTrue { this.realSold == 300.toAmount() } // 1000 - 500 - 200
            result.assertTrue { this.currentTransactions.size == 2 }
            result.assertTrue { this.currentTransactions.none { it.isIncome } }
        }

        @Test
        fun `Should calculate real sold correctly with mixed income and expense transactions`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Mixed Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transaction1 = Transaction(id = UUID.randomUUID(), label = "Income 1", date = LocalDate.of(2025, 2, 5),
                amount = 1500.toAmount(), isIncome = true, isPreview = false)
            val transaction2 = Transaction(id = UUID.randomUUID(), label = "Expense 1", date = LocalDate.of(2025, 2, 10),
                amount = 300.toAmount(), isIncome = false, isPreview = false)
            val transaction3 = Transaction(id = UUID.randomUUID(), label = "Income 2", date = LocalDate.of(2025, 2, 15),
                amount = 800.toAmount(), isIncome = true, isPreview = false)
            val transaction4 = Transaction(id = UUID.randomUUID(), label = "Expense 2", date = java.time.LocalDate.of(2025, 2, 20),
                amount = 500.toAmount(), isIncome = false, isPreview = false)
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(
                    IdUserBooklet(ctx.userId, bookletId),
                    mutableListOf(transaction1, transaction2, transaction3, transaction4)
                )
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.FEBRUARY, 2025,
                startingMonth = java.time.Month.FEBRUARY, startingYear = 2025
            ))
            result.assertTrue { this.realSold == 2500.toAmount() } // 1000 + 1500 - 300 + 800 - 500 = 2500
        }

        @Test
        fun `Should calculate previsional sold correctly with preview transactions`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Preview Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transaction1 = Transaction(id = UUID.randomUUID(), label = "Current Income", date = LocalDate.of(2025, 11, 5),
                amount = 500.toAmount(), isIncome = true, isPreview = false)
            val transaction2 = Transaction(id = UUID.randomUUID(), label = "Future Income", date = LocalDate.of(2025, 11, 25),
                amount = 1000.toAmount(), isIncome = true, isPreview = true)
            val transaction3 = Transaction(id = UUID.randomUUID(), label = "Future Expense", date = LocalDate.of(2025, 11, 28),
                amount = 300.toAmount(), isIncome = false, isPreview = true)
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(transaction1, transaction2, transaction3))
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.NOVEMBER, 2025,
                startingMonth = java.time.Month.NOVEMBER, startingYear = 2025
            ))
            result.assertTrue { this.realSold == 1500.toAmount() } // 1000 + 500 (only current)
            result.assertTrue { this.previsionalSold.value > this.realSold.value } // Should be higher with preview
            result.assertTrue { this.previsionalTransactions.size == 2 }
            result.assertTrue { this.currentTransactions.size == 1 }
        }

        @Test
        fun `Should separate current and previsional transactions correctly`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Separation Test", amount = 2000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transaction1 = Transaction(id = UUID.randomUUID(), label = "Current 1", date = LocalDate.of(2025, 4, 1),
                amount = 100.toAmount(), isIncome = true, isPreview = false)
            val transaction2 = Transaction(id = UUID.randomUUID(), label = "Current 2", date = LocalDate.of(2025, 4, 10),
                amount = 50.toAmount(), isIncome = false, isPreview = false)
            val transaction3 = Transaction(id = UUID.randomUUID(), label = "Current 3", date = LocalDate.of(2025, 4, 15),
                amount = 75.toAmount(), isIncome = true, isPreview = false)
            val transaction4 = Transaction(id = UUID.randomUUID(), label = "Preview 1", date = LocalDate.of(2025, 4, 20),
                amount = 200.toAmount(), isIncome = true, isPreview = true)
            val transaction5 = Transaction(id = UUID.randomUUID(), label = "Preview 2", date = LocalDate.of(2025, 4, 25),
                amount = 150.toAmount(), isIncome = false, isPreview = true)
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(
                    IdUserBooklet(ctx.userId, bookletId),
                    mutableListOf(transaction1, transaction2, transaction3, transaction4, transaction5)
                )
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.APRIL, 2025,
                startingMonth = java.time.Month.APRIL, startingYear = 2025
            ))
            result.assertTrue { this.currentTransactions.size == 3 }
            result.assertTrue { this.previsionalTransactions.size == 2 }
            result.assertTrue { this.currentTransactions.all { !it.isPreview } }
            result.assertTrue { this.previsionalTransactions.all { it.isPreview } }
        }

        @Test
        fun `Should retrieve regular transactions for the booklet`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Regular Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val regularTransaction1 = RegularTransaction(
                label = "Monthly Salary", amount = 3000.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId.value}-salary"),
                startDate = LocalDate.of(2025, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )
            val regularTransaction2 = RegularTransaction(
                label = "Monthly Rent", amount = 800.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId.value}-rent"),
                startDate = LocalDate.of(2025, 1, 5),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(5)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularTransaction1, bookletIds = listOf(bookletId)),
                UserRegularTransaction(userId = ctx.userId, transaction = regularTransaction2, bookletIds = listOf(bookletId))
            ))
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025
            ))
            result.assertTrue { this.regularTransactions.size == 2 }
            result.assertTrue { this.regularTransactions.any { it.label == "Monthly Salary" } }
            result.assertTrue { this.regularTransactions.any { it.label == "Monthly Rent" } }
        }

        @Test
        fun `Should return empty lists when booklet has no transactions`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Empty Booklet", amount = 500.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.MAY, 2025,
                startingMonth = java.time.Month.MAY, startingYear = 2025
            ))
            result.assertTrue { this.currentTransactions.isEmpty() }
            result.assertTrue { this.previsionalTransactions.isEmpty() }
            result.assertTrue { this.realSold == 500.toAmount() }
            result.assertTrue { this.label == "Empty Booklet" }
        }

        @Test
        fun `Should fail when booklet does not exist`() {
            val ctx = scenario.withUser()
            loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, UUID.randomUUID(), // Non-existent booklet ID
                java.time.Month.JUNE, 2025,
                startingMonth = java.time.Month.JUNE, startingYear = 2025
            )).assertFailure()
        }

        @Test
        fun `Should only load transactions for the requested month and year`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Month Filter Test", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transaction1 = Transaction(id = UUID.randomUUID(), label = "January Transaction", date = LocalDate.of(2025, 1, 15),
                amount = 100.toAmount(), isIncome = true, isPreview = false)
            val transaction2 = Transaction(id = UUID.randomUUID(), label = "February Transaction 1", date = java.time.LocalDate.of(2025, 2, 10),
                amount = 200.toAmount(), isIncome = true, isPreview = false)
            val transaction3 = Transaction(id = UUID.randomUUID(), label = "February Transaction 2", date = LocalDate.of(2025, 2, 20),
                amount = 150.toAmount(), isIncome = false, isPreview = false)
            val transaction4 = Transaction(id = UUID.randomUUID(), label = "March Transaction", date = LocalDate.of(2025, 3, 5),
                amount = 50.toAmount(), isIncome = true, isPreview = false)
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(
                    IdUserBooklet(ctx.userId, bookletId),
                    mutableListOf(transaction1, transaction2, transaction3, transaction4)
                )
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.FEBRUARY, 2025,
                startingMonth = java.time.Month.FEBRUARY, startingYear = 2025
            ))
            result.assertTrue { this.currentTransactions.size == 2 }
            result.assertTrue { this.currentTransactions.all { it.date.month == java.time.Month.FEBRUARY } }
            result.assertTrue { this.currentTransactions.all { it.date.year == 2025 } }
            result.assertTrue { this.currentTransactions.any { it.label == "February Transaction 1" } }
            result.assertTrue { this.currentTransactions.any { it.label == "February Transaction 2" } }
        }

        @Test
        fun `Should calculate previsional sold including future months transactions`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Future Sold Test", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transaction1 = Transaction(id = UUID.randomUUID(), label = "Current Income", date = LocalDate.of(2025, 11, 15),
                amount = 500.toAmount(), isIncome = true, isPreview = false)
            val transaction2 = Transaction(id = UUID.randomUUID(), label = "Future Income", date = LocalDate.of(2025, 12, 10),
                amount = 800.toAmount(), isIncome = true, isPreview = true)
            val transaction3 = Transaction(id = UUID.randomUUID(), label = "Future Expense", date = LocalDate.of(2026, 1, 5),
                amount = 300.toAmount(), isIncome = false, isPreview = true)
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(
                    IdUserBooklet(ctx.userId, bookletId),
                    mutableListOf(transaction1, transaction2, transaction3)
                )
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2026,
                startingMonth = java.time.Month.NOVEMBER, startingYear = 2025
            ))
            result.assertTrue { this.realSold == 1500.toAmount() } // 1000 + 500
            result.assertTrue { this.previsionalSold.value > this.realSold.value }
        }

        @Test
        fun `Should reflect add and remove of regular transaction via regularTransactionState`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "RT CRUD Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            factory.regularTransactionState.init(emptyList())

            var result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025
            ))
            result.assertTrue { this.regularTransactions.isEmpty() }

            val regular = RegularTransaction(
                label = "Monthly Income RT", amount = 100.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId.value}-rt1"),
                startDate = LocalDate.of(2025, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regular, bookletIds = listOf(bookletId))
            ))

            result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025
            ))
            result.assertTrue { this.regularTransactions.size == 1 }
            result.assertTrue { this.regularTransactions.any { it.label == "Monthly Income RT" } }

            factory.regularTransactionState.init(emptyList())
            result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025
            ))
            result.assertTrue { this.regularTransactions.isEmpty() }
        }

        @Test
        fun `Should not expose regular transactions of other user (multi-tenant isolation)`() {
            val bookletIdMine = UUID.randomUUID()
            val bookletIdOther = UUID.randomUUID()
            val myBooklet = BookletFixture.aBooklet(id = bookletIdMine, label = "My Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(myBooklet)
            val otherUser = userRepository.register("other${UUID.randomUUID()}", "pw") as User
            val bookletOther = BookletFixture.aBooklet(id = bookletIdOther, label = "Other Booklet", amount = 1000.toAmount(), owner = otherUser)
            factory.bookletState().initWith(BookletsByOwner(listOf(bookletOther), otherUser.id))
            val rtMine = RegularTransaction(
                label = "Mine RT", amount = 50.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId.value}-mine"),
                startDate = LocalDate.of(2025, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )
            val rtOther = RegularTransaction(
                label = "Other RT", amount = 999.toAmount(), isIncome = true,
                id = RegularTransactionId("${otherUser.id.value}-other"),
                startDate = LocalDate.of(2025, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = rtMine, bookletIds = listOf(bookletIdMine)),
                UserRegularTransaction(userId = otherUser.id, transaction = rtOther, bookletIds = listOf(bookletIdOther))
            ))
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletIdMine, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025
            ))
            result.assertTrue { this.regularTransactions.size == 1 }
            result.assertTrue { this.regularTransactions.any { it.label == "Mine RT" } }
            result.assertTrue { this.regularTransactions.none { it.label == "Other RT" } }
        }

        @Test
        fun `Should include regular transactions in previsional sold calculation`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "RT Calculation Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val regularIncome = RegularTransaction(
                label = "Monthly Salary", amount = 2000.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId.value}-salary"),
                startDate = LocalDate.of(2025, 11, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularIncome, bookletIds = listOf(bookletId))
            ))
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.NOVEMBER, 2025,
                startingMonth = java.time.Month.NOVEMBER, startingYear = 2025
            ))
            println(result.mapNotNullOrFailure())
            result.assertTrue { this.regularTransactions.size == 1 }
            result.assertTrue { this.previsionalSold.value >= BigDecimal(3000) }
        }

        @Test
        fun `Should handle multiple regular transactions with different frequencies`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Multi RT Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val rt1 = RegularTransaction(
                label = "RT Start Day 1", amount = 500.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId.value}-rt1"),
                startDate = LocalDate.of(2025, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )
            val rt2 = RegularTransaction(
                label = "RT Start Day 15", amount = 300.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId.value}-rt2"),
                startDate = LocalDate.of(2025, 1, 15),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(15)
            )
            val rt3 = RegularTransaction(
                label = "RT Start Day 25", amount = 200.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId.value}-rt3"),
                startDate = LocalDate.of(2025, 1, 25),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(25)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = rt1, bookletIds = listOf(bookletId)),
                UserRegularTransaction(userId = ctx.userId, transaction = rt2, bookletIds = listOf(bookletId)),
                UserRegularTransaction(userId = ctx.userId, transaction = rt3, bookletIds = listOf(bookletId))
            ))
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025
            ))
            result.assertTrue { this.regularTransactions.size == 3 }
            result.assertTrue { this.regularTransactions.any { it.label == "RT Start Day 1" } }
            result.assertTrue { this.regularTransactions.any { it.label == "RT Start Day 15" } }
            result.assertTrue { this.regularTransactions.any { it.label == "RT Start Day 25" } }
        }

        @Test
        fun `Should combine regular transactions with current and preview transactions correctly`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Combined Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val currentTx = Transaction(id = UUID.randomUUID(), label = "Current Expense", date = LocalDate.of(2025, 12, 5),
                amount = 100.toAmount(), isIncome = false, isPreview = false)
            val previewTx = Transaction(id = UUID.randomUUID(), label = "Preview Income", date = LocalDate.of(2025, 12, 20),
                amount = 500.toAmount(), isIncome = true, isPreview = true)
            val regularTx = RegularTransaction(
                label = "Regular Income", amount = 2000.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId.value}-regular"),
                startDate = LocalDate.of(2025, 10, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(currentTx, previewTx))
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularTx, bookletIds = listOf(bookletId))
            ))
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.DECEMBER, 2025,
                startingMonth = java.time.Month.DECEMBER, startingYear = 2025
            ))
            result.assertTrue { this.regularTransactions.size == 1 }
            result.assertTrue { this.regularTransactions.any { it.label == "Regular Income" } }
            result.assertTrue { this.currentTransactions.isNotEmpty() || this.previsionalTransactions.isNotEmpty() }
            result.assertTrue { this.previsionalSold.value >= this.realSold.value }
        }

        @Test
        fun `Should not include regular transactions that started after the requested month`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Future RT Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val futureRT = RegularTransaction(
                label = "Future RT", amount = 500.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId.value}-future"),
                startDate = LocalDate.of(2025, 3, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = futureRT, bookletIds = listOf(bookletId))
            ))
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025
            ))
            result.assertTrue { this.regularTransactions.isEmpty() }
        }

        @Test
        fun `Should not double count virtual regular transactions when months already have confirmed physical transactions`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "No Double Count", amount = 2000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val regularTransactionId = RegularTransactionId("${ctx.userId.value}-salary")
            val regularIncome = RegularTransaction(
                label = "Monthly Salary", amount = 500.toAmount(), isIncome = true,
                id = regularTransactionId,
                startDate = LocalDate.of(2026, 1, 5),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(5)
            )
            val febConfirmed = Transaction(id = UUID.randomUUID(), label = "Monthly Salary", date = LocalDate.of(2026, 2, 5),
                amount = 500.toAmount(), isIncome = true, isPreview = false, regularTransactionId = regularTransactionId)
            val marConfirmed = Transaction(id = UUID.randomUUID(), label = "Monthly Salary", date = LocalDate.of(2026, 3, 5),
                amount = 500.toAmount(), isIncome = true, isPreview = false, regularTransactionId = regularTransactionId)
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularIncome, bookletIds = listOf(bookletId))
            ))
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(febConfirmed, marConfirmed))
            )
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.MARCH, 2026,
                startingMonth = java.time.Month.FEBRUARY, startingYear = 2026
            ))
            result.assertTrue { this.previsionalSold == this.realSold }
        }

        @Test
        fun `Balances endpoint should not double count virtual regular transactions when confirmed physical already exists`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "No Double Count Balances", amount = 2000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val regularTransactionId = RegularTransactionId("${ctx.userId.value}-salary-balances")
            val regularIncome = RegularTransaction(
                label = "Monthly Salary", amount = 500.toAmount(), isIncome = true,
                id = regularTransactionId,
                startDate = LocalDate.of(2026, 1, 5),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(5)
            )
            val marConfirmed = Transaction(id = UUID.randomUUID(), label = "Monthly Salary", date = LocalDate.of(2026, 3, 5),
                amount = 500.toAmount(), isIncome = true, isPreview = false, regularTransactionId = regularTransactionId)
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularIncome, bookletIds = listOf(bookletId))
            ))
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(marConfirmed))
            )
            val result = loadBalancesForBookletForAMonthService.handle(LoadBalancesForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.MARCH, 2026,
                startingMonth = java.time.Month.MARCH, startingYear = 2026
            ))
            result.assertTrue { this.previewSold == this.realSold }
        }

        @Test
        fun `Balances endpoint should include preview transactions in current-to-target month range`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Range Coverage", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val febPreviewIncome = Transaction(id = UUID.randomUUID(), label = "Projected income", date = LocalDate.of(2026, 2, 10),
                amount = 100.toAmount(), isIncome = true, isPreview = true)
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(febPreviewIncome))
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadBalancesForBookletForAMonthService.handle(LoadBalancesForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.MARCH, 2026,
                startingMonth = java.time.Month.JANUARY, startingYear = 2026
            ))
            result.assertTrue { this.realSold == 1000.toAmount() }
            result.assertTrue { this.previewSold == 1100.toAmount() }
        }

        @Test
        fun `Should not expose null id previsional transactions for current month`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Current Month No Null Id", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val regularTx = RegularTransaction(
                label = "Current month regular", amount = 120.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId.value}-current-null-id-check"),
                startDate = LocalDate.of(2026, 3, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularTx, bookletIds = listOf(bookletId))
            ))
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.MARCH, 2026,
                startingMonth = java.time.Month.MARCH, startingYear = 2026
            ))
            result.assertTrue { this.previsionalTransactions.all { tr -> tr.id != null } }
        }

        @Test
        fun `Should expose virtual previsional transactions with null id for non current month`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Future Month Virtual Id", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val regularTx = RegularTransaction(
                label = "Future month regular", amount = 75.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId.value}-future-null-id-check"),
                startDate = LocalDate.of(2026, 4, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularTx, bookletIds = listOf(bookletId))
            ))
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.APRIL, 2026,
                startingMonth = java.time.Month.MARCH, startingYear = 2026
            ))
            result.assertTrue { this.previsionalTransactions.any { tr -> tr.id == null } }
        }

        @Test
        fun `Explicit date range should include start and exclude end plus one day for transactions`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Explicit Range Tx", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val txAtStart = Transaction(id = UUID.randomUUID(), label = "At start", date = LocalDate.of(2026, 3, 28),
                amount = 100.toAmount(), isIncome = false, isPreview = false)
            val txAtEnd = Transaction(id = UUID.randomUUID(), label = "At end", date = LocalDate.of(2026, 4, 27),
                amount = 50.toAmount(), isIncome = false, isPreview = false)
            val txAfterEnd = Transaction(id = UUID.randomUUID(), label = "After end", date = LocalDate.of(2026, 4, 28),
                amount = 75.toAmount(), isIncome = false, isPreview = false)
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(txAtStart, txAtEnd, txAfterEnd))
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                userId = ctx.userId, bookletId = bookletId, month = java.time.Month.APRIL, year = 2026,
                startDate = LocalDate.of(2026, 3, 28), endDate = LocalDate.of(2026, 4, 27)
            ))
            result.assertTrue { this.currentTransactions.any { tr -> tr.label == "At start" } }
            result.assertTrue { this.currentTransactions.any { tr -> tr.label == "At end" } }
            result.assertTrue { this.currentTransactions.none { tr -> tr.label == "After end" } }
        }

        @Test
        fun `Explicit date range should bound previsional sold to provided end date`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Explicit Range Balance", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val inRangePreview = Transaction(id = UUID.randomUUID(), label = "In range preview", date = LocalDate.of(2026, 4, 27),
                amount = 120.toAmount(), isIncome = true, isPreview = true)
            val outOfRangePreview = Transaction(id = UUID.randomUUID(), label = "Out of range preview", date = LocalDate.of(2026, 4, 28),
                amount = 80.toAmount(), isIncome = true, isPreview = true)
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(inRangePreview, outOfRangePreview))
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadBalancesForBookletForAMonthService.handle(LoadBalancesForBookletForAMonthQuery(
                userId = ctx.userId, bookletId = bookletId, month = java.time.Month.APRIL, year = 2026,
                startDate = LocalDate.of(2026, 3, 28), endDate = LocalDate.of(2026, 4, 27)
            ))
            result.assertTrue { this.realSold == 1000.toAmount() }
            result.assertTrue { this.previewSold == 1120.toAmount() }
        }

        @Test
        fun `Should not generate virtual transactions for a past explicit range with default settings`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Past Default Range", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val regularTx = RegularTransaction(
                label = "Monthly salary", amount = 3000.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId.value}-past-default-range"),
                startDate = LocalDate.of(2026, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(15)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularTx, bookletIds = listOf(bookletId))
            ))
            factory.fakeTransactionRepository().init(emptyList())
            // Simulate: current date is 1st April 2026, user views March 2026 with default settings (1â†’31)
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                userId = ctx.userId, bookletId = bookletId, month = java.time.Month.MARCH, year = 2026,
                startingMonth = java.time.Month.APRIL, startingYear = 2026,
                startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 3, 31)
            ))
            result.assertTrue { this.previsionalTransactions.isEmpty() }
        }

        @Test
        fun `Should not generate virtual transactions for a past custom cycle range`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Past Custom Cycle", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val regularTx = RegularTransaction(
                label = "Monthly salary", amount = 3000.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId.value}-past-custom-cycle"),
                startDate = LocalDate.of(2026, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(28)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularTx, bookletIds = listOf(bookletId))
            ))
            factory.fakeTransactionRepository().init(emptyList())
            // Simulate: current date is 1st April 2026, user views March cycle (28 Feb â†’ 27 Mar)
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                userId = ctx.userId, bookletId = bookletId, month = java.time.Month.MARCH, year = 2026,
                startingMonth = java.time.Month.APRIL, startingYear = 2026,
                startDate = LocalDate.of(2026, 2, 28), endDate = LocalDate.of(2026, 3, 27)
            ))
            result.assertTrue { this.previsionalTransactions.isEmpty() }
        }

        @Test
        fun `Should generate virtual transaction for a future custom cycle range`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Future Custom Cycle", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val regularTx = RegularTransaction(
                label = "Monthly salary", amount = 3000.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId.value}-future-custom-cycle"),
                startDate = LocalDate.of(2026, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(28)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularTx, bookletIds = listOf(bookletId))
            ))
            factory.fakeTransactionRepository().init(emptyList())
            // Simulate: current date is 1st April 2026, user views May cycle (28 Apr â†’ 27 May)
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                userId = ctx.userId, bookletId = bookletId, month = java.time.Month.MAY, year = 2026,
                startingMonth = java.time.Month.APRIL, startingYear = 2026,
                startDate = LocalDate.of(2026, 4, 28), endDate = LocalDate.of(2026, 5, 27)
            ))
            result.assertTrue { this.previsionalTransactions.isNotEmpty() }
            result.assertTrue {
                this.previsionalTransactions.any { tx ->
                    tx.date == LocalDate.of(2026, 4, 28) && tx.isPreview
                }
            }
        }

        @Test
        fun `Should not generate previsional for day outside cycle start boundary with custom date range`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Cycle Offset Start Boundary", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            // Monthly-on-5: without fix, March 5 would be generated by the full-month MARCH loop.
            // With fix, March 5 is before the cycle start (March 28) â†’ must NOT be generated.
            // April 5 is within [March 28, April 27] â†’ must be generated.
            val regularTx = RegularTransaction(
                label = "Rent", amount = 800.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId.value}-rent-cycle-offset-start"),
                startDate = LocalDate.of(2026, 1, 5),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(5)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularTx, bookletIds = listOf(bookletId))
            ))
            factory.fakeTransactionRepository().init(emptyList())
            // Current cycle: March 28 â†’ April 27. startingMonth = APRIL so today = April N.
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                userId = ctx.userId, bookletId = bookletId, month = java.time.Month.APRIL, year = 2026,
                startingMonth = java.time.Month.APRIL, startingYear = 2026,
                startDate = LocalDate.of(2026, 3, 28), endDate = LocalDate.of(2026, 4, 27)
            ))
            result.assertTrue { this.previsionalTransactions.none { tx -> tx.date == LocalDate.of(2026, 3, 5) } }
            result.assertTrue { this.previsionalTransactions.any { tx -> tx.date == LocalDate.of(2026, 4, 5) } }
        }

        @Test
        fun `Should not generate previsional for day outside cycle end boundary with custom date range`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Cycle Offset End Boundary", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            // Monthly-on-29: without fix, April 29 would be generated by the full-month APRIL loop.
            // With fix, April 29 is after the cycle end (April 27) â†’ must NOT be generated.
            // March 29 is within [March 28, April 27] â†’ must be generated.
            val regularTx = RegularTransaction(
                label = "Salary", amount = 3000.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId.value}-salary-cycle-offset-end"),
                startDate = LocalDate.of(2026, 1, 29),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(29)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularTx, bookletIds = listOf(bookletId))
            ))
            factory.fakeTransactionRepository().init(emptyList())
            // Current cycle: March 28 â†’ April 27. startingMonth = APRIL so today = April N.
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                userId = ctx.userId, bookletId = bookletId, month = java.time.Month.APRIL, year = 2026,
                startingMonth = java.time.Month.APRIL, startingYear = 2026,
                startDate = LocalDate.of(2026, 3, 28), endDate = LocalDate.of(2026, 4, 27)
            ))
            result.assertTrue { this.previsionalTransactions.none { tx -> tx.date == LocalDate.of(2026, 4, 29) } }
            result.assertTrue { this.previsionalTransactions.any { tx -> tx.date == LocalDate.of(2026, 3, 29) } }
        }

        @Test
        fun `loadTransactions should signal hasRegenerableTransactions when a month is excluded for a regular transaction`() {
            val customBooklet = BookletFixture.aBooklet(label = "Test Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val bookletId = ctx.booklet.id!!
            val regularTx = RegularTransaction(
                label = "Loyer", amount = 900.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId.value}-has-regen-test"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(5)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularTx, bookletIds = listOf(bookletId))
            ))
            factory.fakeTransactionRepository().init(emptyList())
            val currentDate = LocalDate.now()
            factory.trackerRepository().markMonthAsExcluded(regularTx.id, bookletId, currentDate.year, currentDate.month)
            factory.fakeTransactionRepository().init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                userId = ctx.userId, bookletId = bookletId,
                month = currentDate.month, year = currentDate.year
            ))
            result.assertTrue { this.hasRegenerableTransactions }
        }

        @Test
        fun `loadTransactions should not signal hasRegenerableTransactions when the excluded month is a past month`() {
            val customBooklet = BookletFixture.aBooklet(label = "Test Booklet Past", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val bookletId = ctx.booklet.id!!
            val regularTx = RegularTransaction(
                label = "Loyer", amount = 900.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId.value}-past-regen-test"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(5)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularTx, bookletIds = listOf(bookletId))
            ))
            factory.fakeTransactionRepository().init(emptyList())
            // Mark a past month as excluded
            factory.trackerRepository().markMonthAsExcluded(regularTx.id, bookletId, 2024, java.time.Month.FEBRUARY)
            // Load for that same past month
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                userId = ctx.userId, bookletId = bookletId,
                month = java.time.Month.FEBRUARY, year = 2024,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025
            ))
            result.assertTrue { !this.hasRegenerableTransactions }
        }

        @Test
        fun `loadTransactions should not signal hasRegenerableTransactions when no month is excluded`() {
            val customBooklet = BookletFixture.aBooklet(label = "Test Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val bookletId = ctx.booklet.id!!
            val regularTx = RegularTransaction(
                label = "Loyer", amount = 900.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId.value}-no-regen-test"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(5)
            )
            factory.regularTransactionState.init(listOf(
                UserRegularTransaction(userId = ctx.userId, transaction = regularTx, bookletIds = listOf(bookletId))
            ))
            factory.fakeTransactionRepository().init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                userId = ctx.userId, bookletId = bookletId,
                month = java.time.Month.FEBRUARY, year = 2024,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025
            ))
            result.assertTrue { !this.hasRegenerableTransactions }
        }

    }

    @Nested
    inner class LoadTransactionsWithPaginationTest {

        @Test
        fun `should return first page of transactions when 25 transactions exist`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Paginated Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transactions = (1..25).map { i ->
                Transaction(id = UUID.randomUUID(), label = "Transaction $i",
                    date = LocalDate.of(2025, 1, (i % 28) + 1), amount = 100.toAmount(),
                    isIncome = true, isPreview = false)
            }
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), transactions.toMutableList())
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                pageNumber = 0, pageSize = 10
            ))
            result.assertTrue { currentTransactions.size + previsionalTransactions.size == 10 }
            result.assertTrue { totalElements == 25L }
            result.assertTrue { totalPages == 3 }
            result.assertTrue { pageNumber == 0 }
            result.assertTrue { pageSize == 10 }
        }

        @Test
        fun `should return last page with fewer items when total is not divisible by pageSize`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Last Page Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transactions = (1..25).map { i ->
                Transaction(id = UUID.randomUUID(), label = "Transaction $i",
                    date = LocalDate.of(2025, 1, (i % 28) + 1), amount = 100.toAmount(),
                    isIncome = true, isPreview = false)
            }
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), transactions.toMutableList())
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                pageNumber = 2, pageSize = 10
            ))
            result.assertTrue { currentTransactions.size + previsionalTransactions.size == 5 }
            result.assertTrue { totalElements == 25L }
            result.assertTrue { totalPages == 3 }
        }

        @Test
        fun `should use default page=0 and size=10 when no pagination params are given`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Default Pagination Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transactions = (1..15).map { i ->
                Transaction(id = UUID.randomUUID(), label = "Transaction $i",
                    date = LocalDate.of(2025, 1, (i % 28) + 1), amount = 100.toAmount(),
                    isIncome = true, isPreview = false)
            }
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), transactions.toMutableList())
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025
            ))
            result.assertTrue { pageNumber == 0 }
            result.assertTrue { pageSize == 10 }
            result.assertTrue { currentTransactions.size + previsionalTransactions.size == 10 }
            result.assertTrue { totalElements == 15L }
        }

        @Test
        fun `should return empty transaction list when requested page is out of range`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Out of Range Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transactions = (1..5).map { i ->
                Transaction(id = UUID.randomUUID(), label = "Transaction $i",
                    date = LocalDate.of(2025, 1, i), amount = 100.toAmount(),
                    isIncome = true, isPreview = false)
            }
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), transactions.toMutableList())
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                pageNumber = 5, pageSize = 10
            ))
            result.assertTrue { currentTransactions.isEmpty() && previsionalTransactions.isEmpty() }
            result.assertTrue { totalElements == 5L }
        }

        @Test
        fun `should compute realSold on ALL transactions regardless of requested page`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Balance Invariance Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transactions = (1..20).map { i ->
                Transaction(id = UUID.randomUUID(), label = "Income $i",
                    date = LocalDate.of(2025, 1, (i % 28) + 1), amount = 100.toAmount(),
                    isIncome = true, isPreview = false)
            }
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), transactions.toMutableList())
            )
            factory.regularTransactionState.init(emptyList())
            val page0Result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                pageNumber = 0, pageSize = 10
            ))
            val page1Result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                pageNumber = 1, pageSize = 10
            ))
            // realSold = booklet.amount (stored balance) â€” unaffected by pagination
            page0Result.assertTrue { realSold == page1Result.mapNotNullOrFailure()!!.realSold }
        }
    }

    @Nested
    inner class LoadTransactionsWithDateSortTest {

        private fun initBookletWithFifteenDailyTransactions(): Pair<UUID, UserId> {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Sorted Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val transactions = (1..15).map { day ->
                Transaction(id = UUID.randomUUID(), label = "Transaction $day",
                    date = LocalDate.of(2025, 1, day), amount = 100.toAmount(),
                    isIncome = true, isPreview = false)
            }
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), transactions.toMutableList())
            )
            factory.regularTransactionState.init(emptyList())
            return bookletId to ctx.userId
        }

        @Test
        fun `should sort all transactions of the period by descending date before paginating`() {
            val (bookletId, userId) = initBookletWithFifteenDailyTransactions()
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                pageNumber = 0, pageSize = 10,
                sortDirection = TransactionSortDirection.DESCENDING
            ))
            result.assertTrue { currentTransactions.map { it.date.dayOfMonth } == (15 downTo 6).toList() }
        }

        @Test
        fun `should return the oldest transactions of the whole period on the last descending page`() {
            val (bookletId, userId) = initBookletWithFifteenDailyTransactions()
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                pageNumber = 1, pageSize = 10,
                sortDirection = TransactionSortDirection.DESCENDING
            ))
            result.assertTrue { currentTransactions.map { it.date.dayOfMonth } == (5 downTo 1).toList() }
        }

        @Test
        fun `should sort all transactions of the period by ascending date before paginating`() {
            val (bookletId, userId) = initBookletWithFifteenDailyTransactions()
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                pageNumber = 1, pageSize = 10,
                sortDirection = TransactionSortDirection.ASCENDING
            ))
            result.assertTrue { currentTransactions.map { it.date.dayOfMonth } == (11..15).toList() }
        }

        @Test
        fun `should interleave previsional and confirmed transactions by date when a sort direction is requested`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Mixed Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val confirmed = Transaction(id = UUID.randomUUID(), label = "Confirmed",
                date = LocalDate.of(2025, 1, 20), amount = 100.toAmount(), isIncome = true, isPreview = false)
            val preview = Transaction(id = UUID.randomUUID(), label = "Preview",
                date = LocalDate.of(2025, 1, 10), amount = 100.toAmount(), isIncome = true, isPreview = true)
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(confirmed, preview))
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                pageNumber = 0, pageSize = 1,
                sortDirection = TransactionSortDirection.ASCENDING
            ))
            result.assertTrue { previsionalTransactions.size == 1 && currentTransactions.isEmpty() }
            result.assertTrue { previsionalTransactions.single().date == LocalDate.of(2025, 1, 10) }
        }

        @Test
        fun `should keep the confirmed then previsional display order when no sort direction is requested`() {
            val bookletId = UUID.randomUUID()
            val customBooklet = BookletFixture.aBooklet(id = bookletId, label = "Unsorted Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val confirmed = Transaction(id = UUID.randomUUID(), label = "Confirmed",
                date = LocalDate.of(2025, 1, 20), amount = 100.toAmount(), isIncome = true, isPreview = false)
            val preview = Transaction(id = UUID.randomUUID(), label = "Preview",
                date = LocalDate.of(2025, 1, 10), amount = 100.toAmount(), isIncome = true, isPreview = true)
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(confirmed, preview))
            )
            factory.regularTransactionState.init(emptyList())
            val result = loadTransactionsForBookletForAMonthService.handle(LoadTransactionsForBookletForAMonthQuery(
                ctx.userId, bookletId, java.time.Month.JANUARY, 2025,
                startingMonth = java.time.Month.JANUARY, startingYear = 2025,
                pageNumber = 0, pageSize = 1
            ))
            result.assertTrue { currentTransactions.size == 1 && previsionalTransactions.isEmpty() }
            result.assertTrue { currentTransactions.single().date == LocalDate.of(2025, 1, 20) }
        }
    }

    @Nested
    inner class RegenerateDeletedPrevisionalTransactionsTest {

        private fun buildRegularTransaction(
            userId: UserId,
            bookletId: UUID,
            dayOfMonth: Int = 5,
            startDate: LocalDate = LocalDate.of(2024, 1, 1)
        ): RegularTransaction {
            val tx = RegularTransaction(
                label = "Loyer",
                amount = 900.toAmount(),
                isIncome = false,
                id = RegularTransactionId("${userId.value}-regen-test"),
                startDate = startDate,
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(dayOfMonth)
            )
            factory.regularTransactionState.init(
                listOf(UserRegularTransaction(userId = userId, transaction = tx, bookletIds = listOf(bookletId)))
            )
            return tx
        }

        @Test
        fun `regenerate for a future month should return virtual transactions and unmark the tracker`() {
            val futureDate = LocalDate.now().plusMonths(2)
            val customBooklet = BookletFixture.aBooklet(label = "Future Regen Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val bookletId = ctx.booklet.id!!
            val regularTx = buildRegularTransaction(ctx.userId, bookletId, dayOfMonth = 5)
            factory.trackerRepository().markMonthAsExcluded(regularTx.id, bookletId, futureDate.year, futureDate.month)
            factory.fakeTransactionRepository().init(emptyList())
            val result = regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                userId = ctx.userId, bookletId = bookletId, month = futureDate.month, year = futureDate.year, regularTransactionIds = listOf(regularTx.id)
            ))
            result.assertTrue { this.isNotEmpty() }
            result.assertTrue { this.any { it.label == "Loyer" } }
            val tracker = factory.trackerRepository().findTracker(regularTx.id, bookletId)
            assertFalse(tracker?.excludedMonths?.contains(YearMonth.from(futureDate)) == true,
                "Future month should no longer be excluded after regeneration")
        }

        @Test
        fun `regenerate for a past month should return empty list without modifying the tracker`() {
            val customBooklet = BookletFixture.aBooklet(label = "Past Regen Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val bookletId = ctx.booklet.id!!
            val regularTx = buildRegularTransaction(ctx.userId, bookletId, dayOfMonth = 5)
            val pastYearMonth = YearMonth.of(2024, java.time.Month.MARCH)
            factory.trackerRepository().markMonthAsExcluded(regularTx.id, bookletId, pastYearMonth.year, pastYearMonth.month)
            val result = regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                userId = ctx.userId, bookletId = bookletId, month = pastYearMonth.month, year = pastYearMonth.year, regularTransactionIds = listOf(regularTx.id)
            ))
            result.assertTrue { this.isEmpty() }
            val tracker = factory.trackerRepository().findTracker(regularTx.id, bookletId)
            assertTrue(tracker?.excludedMonths?.contains(pastYearMonth) == true,
                "Past month tracker exclusion must remain unchanged")
        }

        @Test
        fun `regenerate for a non-existing booklet should return BOOKLET_NOT_FOUND`() {
            val ctx = scenario.withUser()
            factory.regularTransactionState.init(emptyList())
            val result = regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                userId = ctx.userId, bookletId = UUID.randomUUID(),
                month = java.time.Month.JANUARY, year = 2024, regularTransactionIds = listOf(RegularTransactionId("unknown"))
            ))
            result.assertFailure(ResultState.BOOKLET_NOT_FOUND)
        }

        @Test
        fun `regenerate for the current month should recreate previsional transactions`() {
            val currentDate = LocalDate.now()
            val customBooklet = BookletFixture.aBooklet(label = "Regen Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val bookletId = ctx.booklet.id!!
            val regularTx = buildRegularTransaction(ctx.userId, bookletId, dayOfMonth = 1)
            factory.fakeTransactionRepository().init(emptyList())
            factory.trackerRepository().markMonthAsExcluded(regularTx.id, bookletId, currentDate.year, currentDate.month)
            val result = regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                userId = ctx.userId, bookletId = bookletId, month = currentDate.month, year = currentDate.year, regularTransactionIds = listOf(regularTx.id)
            ))
            result.assertTrue { this.isNotEmpty() }
            result.assertTrue { this.any { it.label == "Loyer" && it.isPreview } }
            val tracker = factory.trackerRepository().findTracker(regularTx.id, bookletId)
            assertFalse(tracker?.excludedMonths?.contains(YearMonth.from(currentDate)) == true,
                "Current month should no longer be excluded after regeneration")
        }

        @Test
        fun `regenerate should not create duplicate if confirmed transaction already exists for that month`() {
            val currentDate = LocalDate.now()
            val customBooklet = BookletFixture.aBooklet(label = "No Dup Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val bookletId = ctx.booklet.id!!
            val regularTx = buildRegularTransaction(ctx.userId, bookletId, dayOfMonth = 1)
            // Existing confirmed transaction for same regular tx + current month
            val confirmed = Transaction(
                id = UUID.randomUUID(), label = "Loyer", date = currentDate.withDayOfMonth(1),
                amount = 900.toAmount(), isIncome = false, isPreview = false, regularTransactionId = regularTx.id
            )
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(confirmed))
            )
            // Month is excluded (user deleted the preview before confirming)
            factory.trackerRepository().markMonthAsExcluded(regularTx.id, bookletId, currentDate.year, currentDate.month)
            val result = regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                userId = ctx.userId, bookletId = bookletId, month = currentDate.month, year = currentDate.year, regularTransactionIds = listOf(regularTx.id)
            ))
            // No new transaction should be generated because a confirmed one already exists
            result.assertTrue { this.isEmpty() }
        }

        @Test
        fun `regenerate should not create duplicate if preview transaction already exists for that month`() {
            val currentDate = LocalDate.now()
            val customBooklet = BookletFixture.aBooklet(label = "No Dup Preview Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val bookletId = ctx.booklet.id!!
            val regularTx = buildRegularTransaction(ctx.userId, bookletId, dayOfMonth = 1)
            // Existing preview transaction for same regular tx + current month
            val existingPreview = Transaction(
                id = UUID.randomUUID(), label = "Loyer", date = currentDate.withDayOfMonth(1),
                amount = 900.toAmount(), isIncome = false, isPreview = true, regularTransactionId = regularTx.id
            )
            factory.fakeTransactionRepository().initWith(
                IdBookletByTransaction(IdUserBooklet(ctx.userId, bookletId), mutableListOf(existingPreview))
            )
            val result = regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                userId = ctx.userId, bookletId = bookletId, month = currentDate.month, year = currentDate.year, regularTransactionIds = listOf(regularTx.id)
            ))
            result.assertTrue { this.isEmpty() }
        }

        @Test
        fun `regenerate should only unmark the requested month â€” other excluded months remain excluded`() {
            val currentDate = LocalDate.now()
            val nextMonthDate = currentDate.plusMonths(1)
            val customBooklet = BookletFixture.aBooklet(label = "Only One Month Booklet", amount = 1000.toAmount())
            val ctx = scenario.withUser().withBooklet(customBooklet)
            val bookletId = ctx.booklet.id!!
            val regularTx = buildRegularTransaction(ctx.userId, bookletId, dayOfMonth = 1)
            factory.fakeTransactionRepository().init(emptyList())
            // Exclude current month and next month
            factory.trackerRepository().markMonthAsExcluded(regularTx.id, bookletId, currentDate.year, currentDate.month)
            factory.trackerRepository().markMonthAsExcluded(regularTx.id, bookletId, nextMonthDate.year, nextMonthDate.month)
            // Only regenerate current month
            regenerateDeletedPrevisionalTransactionsService.handle(RegenerateDeletedPrevisionalTransactionsCommand(
                userId = ctx.userId, bookletId = bookletId, month = currentDate.month, year = currentDate.year, regularTransactionIds = listOf(regularTx.id)
            )).assertSuccess()
            val tracker = factory.trackerRepository().findTracker(regularTx.id, bookletId)
            assertFalse(
                tracker?.excludedMonths?.contains(YearMonth.from(currentDate)) == true,
                "Current month should no longer be excluded"
            )
            assertTrue(
                tracker?.excludedMonths?.contains(YearMonth.from(nextMonthDate)) == true,
                "Next month should still be excluded"
            )
        }

        private fun buildTwoRegularTransactions(
            userId: UserId,
            bookletId: UUID,
            suffix: String
        ): Pair<RegularTransaction, RegularTransaction> {
            val rent = RegularTransaction(
                label = "Loyer", amount = 900.toAmount(), isIncome = false,
                id = RegularTransactionId("${userId.value}-$suffix-rent"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )
            val salary = RegularTransaction(
                label = "Salaire", amount = 2500.toAmount(), isIncome = true,
                id = RegularTransactionId("${userId.value}-$suffix-salary"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(28)
            )
            factory.regularTransactionState.init(
                listOf(
                    UserRegularTransaction(userId = userId, transaction = rent, bookletIds = listOf(bookletId)),
                    UserRegularTransaction(userId = userId, transaction = salary, bookletIds = listOf(bookletId))
                )
            )
            factory.fakeTransactionRepository().init(emptyList())
            return rent to salary
        }

        private fun isExcluded(regularTransactionId: RegularTransactionId, bookletId: UUID, yearMonth: YearMonth) =
            factory.trackerRepository().findTracker(regularTransactionId, bookletId)
                ?.excludedMonths?.contains(yearMonth) == true

        @Test
        fun `regenerate should restore only the selected regular transactions and leave the others excluded`() {
            val currentDate = LocalDate.now()
            val currentYearMonth = YearMonth.from(currentDate)
            val ctx = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "Selective Booklet", amount = 1000.toAmount()))
            val bookletId = ctx.booklet.id!!
            val (rent, salary) = buildTwoRegularTransactions(ctx.userId, bookletId, "selective")
            factory.trackerRepository().markMonthAsExcluded(rent.id, bookletId, currentDate.year, currentDate.month)
            factory.trackerRepository().markMonthAsExcluded(salary.id, bookletId, currentDate.year, currentDate.month)

            val result = regenerateDeletedPrevisionalTransactionsService.handle(
                RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = ctx.userId, bookletId = bookletId,
                    month = currentDate.month, year = currentDate.year,
                    regularTransactionIds = listOf(rent.id)
                )
            )

            result.assertTrue { isNotEmpty() }
            result.assertTrue { all { transaction -> transaction.label == "Loyer" } }
            assertFalse(isExcluded(rent.id, bookletId, currentYearMonth), "Selected regular transaction must be restored")
            assertTrue(isExcluded(salary.id, bookletId, currentYearMonth), "Unselected regular transaction must remain excluded")
        }

        @Test
        fun `regenerate for a future month should return only the selected virtual transactions`() {
            val futureDate = LocalDate.now().plusMonths(2)
            val futureYearMonth = YearMonth.from(futureDate)
            val ctx = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "Selective Future", amount = 1000.toAmount()))
            val bookletId = ctx.booklet.id!!
            val (rent, salary) = buildTwoRegularTransactions(ctx.userId, bookletId, "selective-future")
            factory.trackerRepository().markMonthAsExcluded(rent.id, bookletId, futureDate.year, futureDate.month)
            factory.trackerRepository().markMonthAsExcluded(salary.id, bookletId, futureDate.year, futureDate.month)

            val result = regenerateDeletedPrevisionalTransactionsService.handle(
                RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = ctx.userId, bookletId = bookletId,
                    month = futureDate.month, year = futureDate.year,
                    regularTransactionIds = listOf(salary.id)
                )
            )

            result.assertTrue { isNotEmpty() }
            result.assertTrue { all { transaction -> transaction.label == "Salaire" } }
            assertFalse(isExcluded(salary.id, bookletId, futureYearMonth), "Selected regular transaction must be restored")
            assertTrue(isExcluded(rent.id, bookletId, futureYearMonth), "Unselected regular transaction must remain excluded")
        }

        @Test
        fun `regenerate with every excluded identifier should restore them all`() {
            val currentDate = LocalDate.now()
            val currentYearMonth = YearMonth.from(currentDate)
            val ctx = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "Select All Booklet", amount = 1000.toAmount()))
            val bookletId = ctx.booklet.id!!
            val (rent, salary) = buildTwoRegularTransactions(ctx.userId, bookletId, "select-all")
            factory.trackerRepository().markMonthAsExcluded(rent.id, bookletId, currentDate.year, currentDate.month)
            factory.trackerRepository().markMonthAsExcluded(salary.id, bookletId, currentDate.year, currentDate.month)

            val result = regenerateDeletedPrevisionalTransactionsService.handle(
                RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = ctx.userId, bookletId = bookletId,
                    month = currentDate.month, year = currentDate.year,
                    regularTransactionIds = listOf(rent.id, salary.id)
                )
            )

            result.assertTrue { any { transaction -> transaction.label == "Loyer" } }
            result.assertTrue { any { transaction -> transaction.label == "Salaire" } }
            assertFalse(isExcluded(rent.id, bookletId, currentYearMonth))
            assertFalse(isExcluded(salary.id, bookletId, currentYearMonth))
        }

        @Test
        fun `regenerate should ignore a selected identifier that is not excluded for that month`() {
            val currentDate = LocalDate.now()
            val currentYearMonth = YearMonth.from(currentDate)
            val ctx = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "Not Excluded Booklet", amount = 1000.toAmount()))
            val bookletId = ctx.booklet.id!!
            val (rent, salary) = buildTwoRegularTransactions(ctx.userId, bookletId, "not-excluded")
            factory.trackerRepository().markMonthAsExcluded(rent.id, bookletId, currentDate.year, currentDate.month)

            val result = regenerateDeletedPrevisionalTransactionsService.handle(
                RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = ctx.userId, bookletId = bookletId,
                    month = currentDate.month, year = currentDate.year,
                    regularTransactionIds = listOf(salary.id)
                )
            )

            result.assertTrue { isEmpty() }
            assertTrue(isExcluded(rent.id, bookletId, currentYearMonth), "Unselected regular transaction must remain excluded")
        }

        @Test
        fun `regenerate on a booklet belonging to another user should be rejected`() {
            val currentDate = LocalDate.now()
            val owner = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "Foreign Regen Booklet", amount = 1000.toAmount()))
            val bookletId = owner.booklet.id!!
            val (rent, _) = buildTwoRegularTransactions(owner.userId, bookletId, "foreign")
            factory.trackerRepository().markMonthAsExcluded(rent.id, bookletId, currentDate.year, currentDate.month)
            val intruder = scenario.withUser(
                UserFixture.aUserWithPassword(user = UserFixture.aUser(username = "intruder", email = "intruder@jmanager.fr"))
            )

            val result = regenerateDeletedPrevisionalTransactionsService.handle(
                RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = intruder.userId, bookletId = bookletId,
                    month = currentDate.month, year = currentDate.year,
                    regularTransactionIds = listOf(rent.id)
                )
            )

            result.assertFailure(ResultState.BOOKLET_NOT_FOUND)
            assertTrue(
                isExcluded(rent.id, bookletId, YearMonth.from(currentDate)),
                "A foreign user must not be able to restore someone else's transactions"
            )
        }

        @Test
        fun `regenerate with an empty selection should be rejected`() {
            val currentDate = LocalDate.now()
            val ctx = scenario.withUser().withBooklet(BookletFixture.aBooklet(label = "Empty Selection Booklet", amount = 1000.toAmount()))
            val bookletId = ctx.booklet.id!!
            buildTwoRegularTransactions(ctx.userId, bookletId, "empty-selection")

            val result = regenerateDeletedPrevisionalTransactionsService.handle(
                RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = ctx.userId, bookletId = bookletId,
                    month = currentDate.month, year = currentDate.year,
                    regularTransactionIds = emptyList()
                )
            )

            result.assertFailure(ResultState.TRANSACTION_ENTRY_ERROR)
        }
    }
}

