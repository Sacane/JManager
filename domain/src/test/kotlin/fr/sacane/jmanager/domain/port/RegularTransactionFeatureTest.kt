package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.act
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.BookletsByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fake.UserRegularTransaction
import fr.sacane.jmanager.domain.fixture.BookletFixture
import fr.sacane.jmanager.domain.fixture.RegularTransactionFixture
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.input.regularTransaction.*
import fr.sacane.jmanager.domain.then
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class RegularTransactionFeatureTest {

    private val factory = FakeFactory()
    private val scenario = TestScenario(factory)
    private val getAllRegularTransactionsUseCase = factory.getAllRegularTransactionsService
    private val bookRegularTransactionUseCase = factory.bookRegularTransactionService
    private val getRegularTransactionByIdUseCase = factory.getRegularTransactionByIdService
    private val updateRegularTransactionUseCase = factory.updateRegularTransactionService
    private val deleteRegularTransactionUseCase = factory.deleteRegularTransactionService
    private val deleteRegularTransactionsUseCase = factory.deleteRegularTransactionsService
    private val linkRegularTransactionToBookletUseCase = factory.linkRegularTransactionToBookletService
    private val unlinkRegularTransactionFromBookletUseCase = factory.unlinkRegularTransactionFromBookletService
    private val regularTransactionState = factory.regularTransactionState

    @AfterEach
    fun clearUp() {
        factory.clearAll()
    }

    @Nested
    inner class GetAllRegularTransactionsTest {

        @Test
        fun `should retrieve all regular transactions for authenticated user`() {
            val ctx = scenario.withUser().withBooklet().withRegularTransactions(
                RegularTransactionFixture.aMonthlyTransaction(label = "monthly salary", amount = 2500.toAmount(), isIncome = true),
                RegularTransactionFixture.aMonthlyTransaction(label = "Rent", amount = 800.toAmount(), isIncome = false, dayOfMonth = 5)
            )

            val result = act { getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 0, 10)) }

            then(result) {
                assertSuccess()
                onSuccess { page ->
                    assertEquals(2, page.content.size)
                    assertTrue(page.content.any { it.label == "monthly salary" })
                    assertTrue(page.content.any { it.label == "Rent" })
                }
            }
        }

        @Test
        fun `should return empty list when user has no regular transactions`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 0, 10)) }

            then(result) {
                assertSuccess()
                onSuccess { page -> assertTrue(page.content.isEmpty()) }
            }
        }

        @Test
        fun `should return empty page when user has no regular transactions`() {
            val result = act { getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(UserId(UUID.randomUUID()), 0, 10)) }

            then(result) {
                assertSuccess()
                onSuccess { page -> assertTrue(page.content.isEmpty()) }
            }
        }
    }

    @Nested
    inner class GetAllRegularTransactionsPaginatedTest {

        @Test
        fun `should return first page of regular transactions with correct metadata`() {
            val ctx = scenario.withUser().withBooklet().withRegularTransactions(
                (1..35).map { i -> RegularTransactionFixture.aMonthlyTransaction(label = "Transaction $i", amount = 100.toAmount(), isIncome = true) }
            )

            val result = act { getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 0, 10)) }

            then(result) {
                assertSuccess()
                onSuccess { page ->
                    assertEquals(10, page.content.size)
                    assertEquals(35L, page.totalElements)
                    assertEquals(4, page.totalPages)
                    assertEquals(0, page.pageNumber)
                    assertEquals(10, page.pageSize)
                }
            }
        }

        @Test
        fun `should return last page with remaining items when total is not divisible by pageSize`() {
            val ctx = scenario.withUser().withBooklet().withRegularTransactions(
                (1..35).map { i -> RegularTransactionFixture.aMonthlyTransaction(label = "Transaction $i", amount = 100.toAmount(), isIncome = false) }
            )

            val result = act { getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 3, 10)) }

            then(result) {
                assertSuccess()
                onSuccess { page ->
                    assertEquals(5, page.content.size)
                    assertEquals(35L, page.totalElements)
                    assertEquals(4, page.totalPages)
                }
            }
        }

        @Test
        fun `should use default pagination when no page params are provided`() {
            val ctx = scenario.withUser().withBooklet().withRegularTransactions(
                (1..5).map { i -> RegularTransactionFixture.aMonthlyTransaction(label = "RT $i", amount = 50.toAmount(), isIncome = true) }
            )

            val result = act { getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 0, 10)) }

            then(result) {
                assertSuccess()
                onSuccess { page ->
                    assertEquals(0, page.pageNumber)
                    assertEquals(10, page.pageSize)
                    assertEquals(5, page.content.size)
                    assertEquals(5L, page.totalElements)
                }
            }
        }

        @Test
        fun `should return empty content when page is out of range`() {
            val ctx = scenario.withUser().withBooklet().withRegularTransactions(
                (1..5).map { i -> RegularTransactionFixture.aMonthlyTransaction(label = "RT $i", amount = 50.toAmount(), isIncome = true) }
            )

            val result = act { getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 10, 10)) }

            then(result) {
                assertSuccess()
                onSuccess { page ->
                    assertTrue(page.content.isEmpty())
                    assertEquals(5L, page.totalElements)
                }
            }
        }
    }

    @Nested
    inner class BookRegularTransactionTest {

        @Test
        fun `should book a monthly transaction with Forever frequency`() {
            val ctx = scenario.withUser().withBooklet()
            val monthlyTransaction = RegularTransactionFixture.aMonthlyTransaction(
                label = "Abonnement Netflix", amount = 15.99.toAmount(), isIncome = false
            )

            val result = act { bookRegularTransactionUseCase.handle(BookRegularTransactionCommand(ctx.userId, monthlyTransaction, listOf(ctx.booklet.id!!))) }

            then(result) {
                assertSuccess()
                onSuccess { savedTransaction ->
                    assertEquals("Abonnement Netflix", savedTransaction.label)
                    assertEquals(15.99.toAmount(), savedTransaction.amount)
                    assertFalse(savedTransaction.isIncome)
                    assertNotNull(savedTransaction.id)
                }
            }
        }

        @Test
        fun `should book a monthly transaction with UntilDate frequency`() {
            val ctx = scenario.withUser().withBooklet()
            val endDate = LocalDate.of(2024, 12, 31)
            val monthlyTransaction = RegularTransactionFixture.aMonthlyTransaction(
                label = "Prêt temporaire", amount = 200.toAmount(), isIncome = false,
                frequencyProperty = FrequencyProperty.UntilDate(endDate), dayOfMonth = 15
            )

            val result = act { bookRegularTransactionUseCase.handle(BookRegularTransactionCommand(ctx.userId, monthlyTransaction, listOf(ctx.booklet.id!!))) }

            then(result) {
                assertSuccess()
                onSuccess { savedTransaction ->
                    assertEquals("Prêt temporaire", savedTransaction.label)
                    assertTrue(savedTransaction.frequencyProperty is FrequencyProperty.UntilDate)
                    assertEquals(endDate, (savedTransaction.frequencyProperty as FrequencyProperty.UntilDate).date)
                }
            }
        }

        @Test
        fun `should book a monthly transaction with SpecificRepetitionTimes frequency`() {
            val ctx = scenario.withUser().withBooklet()
            val monthlyTransaction = RegularTransactionFixture.aMonthlyTransaction(
                label = "Cours de yoga - 10 séances", amount = 50.toAmount(), isIncome = false,
                frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(10), dayOfMonth = 10
            )

            val result = act { bookRegularTransactionUseCase.handle(BookRegularTransactionCommand(ctx.userId, monthlyTransaction, listOf(ctx.booklet.id!!))) }

            then(result) {
                assertSuccess()
                onSuccess { savedTransaction ->
                    assertEquals("Cours de yoga - 10 séances", savedTransaction.label)
                    assertTrue(savedTransaction.frequencyProperty is FrequencyProperty.SpecificRepetitionTimes)
                    assertEquals(10, (savedTransaction.frequencyProperty as FrequencyProperty.SpecificRepetitionTimes).number)
                }
            }
        }

        @Test
        fun `should book a monthly transaction linked to multiple booklets`() {
            val ctx = scenario.withUser().withBooklet()
            val secondBooklet = BookletFixture.aBooklet(owner = ctx.user, label = "Compte épargne", amount = 1000.toAmount())
            factory.bookletState().initWith(BookletsByOwner(listOf(ctx.booklet, secondBooklet), ctx.userId))
            val monthlyTransaction = RegularTransactionFixture.aMonthlyTransaction(
                label = "Épargne automatique", amount = 300.toAmount(), isIncome = true
            )

            val result = act { bookRegularTransactionUseCase.handle(BookRegularTransactionCommand(ctx.userId, monthlyTransaction, listOf(ctx.booklet.id!!, secondBooklet.id!!))) }

            then(result) {
                assertSuccess()
                onSuccess { savedTransaction -> assertNotNull(savedTransaction.id) }
            }
        }
    }

    @Nested
    inner class GetRegularTransactionByIdTest {

        @Test
        fun `should retrieve regular transaction by id`() {
            val transaction = RegularTransactionFixture.aMonthlyTransaction(label = "Abonnement Spotify", amount = 9.99.toAmount(), isIncome = false)
            val ctx = scenario.withUser().withBooklet().withRegularTransactions(transaction)

            val result = act { getRegularTransactionByIdUseCase.handle(GetRegularTransactionByIdQuery(ctx.userId, transaction.id.value)) }

            then(result) {
                assertSuccess()
                onSuccess { retrieved ->
                    assertEquals("Abonnement Spotify", retrieved.label)
                    assertEquals(9.99.toAmount(), retrieved.amount)
                    assertEquals(transaction.id, retrieved.id)
                }
            }
        }

        @Test
        fun `should fail when transaction id does not exist`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { getRegularTransactionByIdUseCase.handle(GetRegularTransactionByIdQuery(ctx.userId, "non-existing-id")) }

            then(result) {
                assertFailure()
                assertEquals("domain.regular_transaction.get_by_id.not_found", errorInfo?.key)
            }
        }
    }

    @Nested
    inner class DeleteRegularTransactionTest {

        @Test
        fun `should delete a regular transaction successfully`() {
            val transaction = RegularTransactionFixture.aMonthlyTransaction(label = "Abonnement à supprimer", amount = 19.99.toAmount(), isIncome = false)
            val ctx = scenario.withUser().withBooklet().withRegularTransactions(transaction)

            val result = act { deleteRegularTransactionUseCase.handle(DeleteRegularTransactionCommand(ctx.userId, transaction.id.value)) }

            then(result) {
                assertSuccess()
                onSuccess { deleted -> assertTrue(deleted) }
            }
            getRegularTransactionByIdUseCase.handle(GetRegularTransactionByIdQuery(ctx.userId, transaction.id.value))
                .assertFailure()
        }

        @Test
        fun `should fail when deleting non-existing transaction`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { deleteRegularTransactionUseCase.handle(DeleteRegularTransactionCommand(ctx.userId, "non-existing-id")) }

            then(result) {
                assertFailure()
                assertEquals("domain.regular_transaction.delete.not_found", errorInfo?.key)
            }
        }

        @Test
        fun `should not delete transaction belonging to another user`() {
            val ctx = scenario.withUser().withBooklet()
            val otherUserId = UserId(UUID.randomUUID())
            val transaction = RegularTransactionFixture.aMonthlyTransaction(label = "Transaction d'un autre utilisateur", amount = 25.toAmount(), isIncome = false)
            regularTransactionState.init(listOf(UserRegularTransaction(otherUserId, transaction)))

            val result = act { deleteRegularTransactionUseCase.handle(DeleteRegularTransactionCommand(ctx.userId, transaction.id.value)) }

            then(result) {
                assertFailure()
                assertEquals("domain.regular_transaction.delete.not_found", errorInfo?.key)
            }
        }

        @Test
        fun `should delete multiple regular transactions successfully`() {
            val first = RegularTransactionFixture.aMonthlyTransaction(label = "Transaction 1", amount = 10.toAmount(), isIncome = false)
            val second = RegularTransactionFixture.aMonthlyTransaction(label = "Transaction 2", amount = 20.toAmount(), isIncome = true, dayOfMonth = 2)
            val ctx = scenario.withUser().withBooklet().withRegularTransactions(first, second)

            val result = act { deleteRegularTransactionsUseCase.handle(DeleteRegularTransactionsCommand(ctx.userId, listOf(first.id.value, second.id.value))) }

            then(result) {
                assertSuccess()
                onSuccess { deletedIds ->
                    assertEquals(2, deletedIds.size)
                    assertTrue(deletedIds.contains(first.id.value))
                    assertTrue(deletedIds.contains(second.id.value))
                }
            }
            then(getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 0, 10))) {
                assertSuccess()
                onSuccess { all ->
                    assertTrue(all.content.none { it.id == first.id })
                    assertTrue(all.content.none { it.id == second.id })
                }
            }
        }

        @Test
        fun `should fail bulk deletion when selection is empty`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { deleteRegularTransactionsUseCase.handle(DeleteRegularTransactionsCommand(ctx.userId, emptyList())) }

            then(result) {
                assertFailure(ResultState.TRANSACTION_ENTRY_ERROR)
                assertEquals("domain.regular_transaction.delete.bulk.empty_selection", errorInfo?.key)
            }
        }

        @Test
        fun `should fail bulk deletion when one transaction is missing`() {
            val first = RegularTransactionFixture.aMonthlyTransaction(label = "Transaction 1", amount = 10.toAmount(), isIncome = false)
            val ctx = scenario.withUser().withBooklet().withRegularTransactions(first)

            val result = act { deleteRegularTransactionsUseCase.handle(DeleteRegularTransactionsCommand(ctx.userId, listOf(first.id.value, "missing-id"))) }

            then(result) {
                assertFailure(ResultState.TRANSACTION_NOT_FOUND)
                assertEquals("domain.regular_transaction.delete.bulk.not_found", errorInfo?.key)
            }
            getRegularTransactionByIdUseCase.handle(GetRegularTransactionByIdQuery(ctx.userId, first.id.value))
                .assertSuccess()
        }
    }

    @Nested
    inner class UpdateRegularTransactionTest {
        @Test
        fun `should patch a regular transaction correctly`() {
            val original = RegularTransactionFixture.aMonthlyTransaction(label = "Abonnement Gym", amount = 40.toAmount(), isIncome = false)
            val ctx = scenario.withUser().withBooklet().withRegularTransactions(original)
            val updated = original.copy(amount = 45.toAmount(), label = "Abonnement Gym Premium")

            val result = act { updateRegularTransactionUseCase.handle(UpdateRegularTransactionCommand(ctx.userId, updated, listOf(ctx.booklet.id!!))) }

            then(result) {
                assertSuccess()
                onSuccess { transaction ->
                    assertEquals("Abonnement Gym Premium", transaction.label)
                    assertEquals(45.toAmount(), transaction.amount)
                    assertEquals(original.id, transaction.id)
                }
            }
        }

        @Test
        fun `should fail when updating unknown regular transaction`() {
            val ctx = scenario.withUser().withBooklet()
            val unknownTransaction = RegularTransactionFixture.aMonthlyTransaction(
                id = RegularTransactionId("unknown-id"),
                label = "Unknown", amount = 45.toAmount(), isIncome = false
            )

            val result = act { updateRegularTransactionUseCase.handle(UpdateRegularTransactionCommand(ctx.userId, unknownTransaction, listOf(ctx.booklet.id!!))) }

            then(result) {
                assertFailure(ResultState.TRANSACTION_NOT_FOUND)
                assertEquals("domain.regular_transaction.update.not_found", errorInfo?.key)
            }
        }
    }

    @Nested
    inner class LinkBookletTest {

        @Test
        fun `should link a booklet to a regular transaction successfully`() {
            val transaction = RegularTransactionFixture.aMonthlyTransaction(label = "Salaire", amount = 2500.toAmount(), isIncome = true)
            val ctx = scenario.withUser().withBooklet().withRegularTransactions(transaction)

            val result = act { linkRegularTransactionToBookletUseCase.handle(LinkRegularTransactionToBookletCommand(ctx.userId, transaction.id.value, ctx.booklet.id!!)) }

            then(result) {
                assertSuccess()
                onSuccess { updated ->
                    assertTrue(updated.associatedBooklets.any { it.id == ctx.booklet.id })
                }
            }
        }

        @Test
        fun `should fail when linking booklet to a non-existing transaction`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { linkRegularTransactionToBookletUseCase.handle(LinkRegularTransactionToBookletCommand(ctx.userId, "non-existing-id", ctx.booklet.id!!)) }

            then(result) {
                assertFailure(ResultState.TRANSACTION_NOT_FOUND)
                assertEquals("domain.regular_transaction.link.not_found", errorInfo?.key)
            }
        }

        @Test
        fun `should fail when linking an already linked booklet`() {
            val ctx = scenario.withUser().withBooklet()
            val transaction = RegularTransactionFixture.aMonthlyTransaction(
                label = "Loyer", amount = 700.toAmount(), isIncome = false,
                dayOfMonth = 5, associatedBooklets = listOf(ctx.booklet)
            )
            ctx.withRegularTransactions(transaction)

            val result = act { linkRegularTransactionToBookletUseCase.handle(LinkRegularTransactionToBookletCommand(ctx.userId, transaction.id.value, ctx.booklet.id!!)) }

            then(result) {
                assertFailure(ResultState.BAD_REQUEST)
                assertEquals("domain.regular_transaction.link.already_linked", errorInfo?.key)
            }
        }
    }

    @Nested
    inner class UnlinkBookletTest {

        @Test
        fun `should unlink a booklet from a regular transaction successfully`() {
            val ctx = scenario.withUser().withBooklet()
            val transaction = RegularTransactionFixture.aMonthlyTransaction(
                label = "Abonnement streaming", amount = 12.99.toAmount(), isIncome = false,
                associatedBooklets = listOf(ctx.booklet)
            )
            ctx.withRegularTransactions(transaction)

            val result = act { unlinkRegularTransactionFromBookletUseCase.handle(UnlinkRegularTransactionFromBookletCommand(ctx.userId, transaction.id.value, ctx.booklet.id!!)) }

            then(result) {
                assertSuccess()
                onSuccess { updated ->
                    assertTrue(updated.associatedBooklets.none { it.id == ctx.booklet.id })
                }
            }
        }

        @Test
        fun `should fail when unlinking booklet from a non-existing transaction`() {
            val ctx = scenario.withUser().withBooklet()

            val result = act { unlinkRegularTransactionFromBookletUseCase.handle(UnlinkRegularTransactionFromBookletCommand(ctx.userId, "non-existing-id", ctx.booklet.id!!)) }

            then(result) {
                assertFailure(ResultState.TRANSACTION_NOT_FOUND)
                assertEquals("domain.regular_transaction.unlink.not_found", errorInfo?.key)
            }
        }

        @Test
        fun `should fail when unlinking a booklet that is not linked`() {
            val transaction = RegularTransactionFixture.aMonthlyTransaction(label = "Dépense non liée", amount = 50.toAmount(), isIncome = false)
            val ctx = scenario.withUser().withBooklet().withRegularTransactions(transaction)

            val result = act { unlinkRegularTransactionFromBookletUseCase.handle(UnlinkRegularTransactionFromBookletCommand(ctx.userId, transaction.id.value, ctx.booklet.id!!)) }

            then(result) {
                assertFailure(ResultState.BAD_REQUEST)
                assertEquals("domain.regular_transaction.unlink.not_linked", errorInfo?.key)
            }
        }
    }
}
