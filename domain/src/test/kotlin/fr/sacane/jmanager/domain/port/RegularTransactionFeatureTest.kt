package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.act
import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.BookletsByOwner
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.TestScenario
import fr.sacane.jmanager.domain.fake.UserRegularTransaction
import fr.sacane.jmanager.domain.given
import fr.sacane.jmanager.domain.initWith
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
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
            val ctx = given {
                val inCtx = scenario.withUser().withBooklet()

                val monthlyTransaction1 = RegularTransaction(
                    label = "monthly salary", amount = 2500.toAmount(), isIncome = true,
                    id = RegularTransactionId("${inCtx.userId}-monthly-1"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
                )
                val monthlyTransaction2 = RegularTransaction(
                    label = "Rent", amount = 800.toAmount(), isIncome = false,
                    id = RegularTransactionId("${inCtx.userId}-monthly-2"),
                    startDate = LocalDate.of(2024, 1, 5),
                    frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(5)
                )
                regularTransactionState.init(listOf(UserRegularTransaction(inCtx.userId, monthlyTransaction1), UserRegularTransaction(inCtx.userId, monthlyTransaction2)))
                inCtx
            }


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
            val ctx = given { scenario.withUser().withBooklet() }

            val result = getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 0, 10))

            result.assertSuccess()
            result.onSuccess { page -> assertTrue(page.content.isEmpty()) }
        }

        @Test
        fun `should return empty page when user has no regular transactions`() {
            val result = getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(UserId(UUID.randomUUID()), 0, 10))
            result.assertSuccess()
            result.onSuccess { page -> assertTrue(page.content.isEmpty()) }
        }
    }

    @Nested
    inner class GetAllRegularTransactionsPaginatedTest {

        @Test
        fun `should return first page of regular transactions with correct metadata`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val transactions = (1..35).map { i ->
                RegularTransaction(
                    label = "Transaction $i", amount = 100.toAmount(), isIncome = true,
                    id = RegularTransactionId("${ctx.userId}-rt-$i"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
                )
            }
            regularTransactionState.init(transactions.map { UserRegularTransaction(ctx.userId, it) })

            val result = getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 0, 10))

            result.assertSuccess()
            result.onSuccess { page ->
                assertEquals(10, page.content.size)
                assertEquals(35L, page.totalElements)
                assertEquals(4, page.totalPages)
                assertEquals(0, page.pageNumber)
                assertEquals(10, page.pageSize)
            }
        }

        @Test
        fun `should return last page with remaining items when total is not divisible by pageSize`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val transactions = (1..35).map { i ->
                RegularTransaction(
                    label = "Transaction $i", amount = 100.toAmount(), isIncome = false,
                    id = RegularTransactionId("${ctx.userId}-rt-last-$i"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
                )
            }
            regularTransactionState.init(transactions.map { UserRegularTransaction(ctx.userId, it) })

            val result = getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 3, 10))

            result.assertSuccess()
            result.onSuccess { page ->
                assertEquals(5, page.content.size)
                assertEquals(35L, page.totalElements)
                assertEquals(4, page.totalPages)
            }
        }

        @Test
        fun `should use default pagination when no page params are provided`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val transactions = (1..5).map { i ->
                RegularTransaction(
                    label = "RT $i", amount = 50.toAmount(), isIncome = true,
                    id = RegularTransactionId("${ctx.userId}-rt-default-$i"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
                )
            }
            regularTransactionState.init(transactions.map { UserRegularTransaction(ctx.userId, it) })

            val result = getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 0, 10))

            result.assertSuccess()
            result.onSuccess { page ->
                assertEquals(0, page.pageNumber)
                assertEquals(10, page.pageSize)
                assertEquals(5, page.content.size)
                assertEquals(5L, page.totalElements)
            }
        }

        @Test
        fun `should return empty content when page is out of range`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val transactions = (1..5).map { i ->
                RegularTransaction(
                    label = "RT $i", amount = 50.toAmount(), isIncome = true,
                    id = RegularTransactionId("${ctx.userId}-rt-oor-$i"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
                )
            }
            regularTransactionState.init(transactions.map { UserRegularTransaction(ctx.userId, it) })

            val result = getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 10, 10))

            result.assertSuccess()
            result.onSuccess { page ->
                assertTrue(page.content.isEmpty())
                assertEquals(5L, page.totalElements)
            }
        }
    }

    @Nested
    inner class BookRegularTransactionTest {

        @Test
        fun `should book a monthly transaction with Forever frequency`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val monthlyTransaction = RegularTransaction(
                label = "Abonnement Netflix", amount = 15.99.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId}-monthly-netflix"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val result = bookRegularTransactionUseCase.handle(BookRegularTransactionCommand(ctx.userId, monthlyTransaction, listOf(ctx.booklet.id!!)))

            result.assertSuccess()
            result.onSuccess { savedTransaction ->
                assertEquals("Abonnement Netflix", savedTransaction.label)
                assertEquals(15.99.toAmount(), savedTransaction.amount)
                assertFalse(savedTransaction.isIncome)
                assertNotNull(savedTransaction.id)
            }
        }

        @Test
        fun `should book a monthly transaction with UntilDate frequency`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val endDate = LocalDate.of(2024, 12, 31)
            val monthlyTransaction = RegularTransaction(
                label = "Prêt temporaire", amount = 200.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId}-monthly-loan"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.UntilDate(endDate), recurrenceRule = RecurrenceRule.Monthly(15)
            )

            val result = bookRegularTransactionUseCase.handle(BookRegularTransactionCommand(ctx.userId, monthlyTransaction, listOf(ctx.booklet.id!!)))

            result.assertSuccess()
            result.onSuccess { savedTransaction ->
                assertEquals("Prêt temporaire", savedTransaction.label)
                assertTrue(savedTransaction.frequencyProperty is FrequencyProperty.UntilDate)
                assertEquals(endDate, (savedTransaction.frequencyProperty as FrequencyProperty.UntilDate).date)
            }
        }

        @Test
        fun `should book a monthly transaction with SpecificRepetitionTimes frequency`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val monthlyTransaction = RegularTransaction(
                label = "Cours de yoga - 10 séances", amount = 50.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId}-monthly-yoga"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(10), recurrenceRule = RecurrenceRule.Monthly(10)
            )

            val result = bookRegularTransactionUseCase.handle(BookRegularTransactionCommand(ctx.userId, monthlyTransaction, listOf(ctx.booklet.id!!)))

            result.assertSuccess()
            result.onSuccess { savedTransaction ->
                assertEquals("Cours de yoga - 10 séances", savedTransaction.label)
                assertTrue(savedTransaction.frequencyProperty is FrequencyProperty.SpecificRepetitionTimes)
                assertEquals(10, (savedTransaction.frequencyProperty as FrequencyProperty.SpecificRepetitionTimes).number)
            }
        }

        @Test
        fun `should book a monthly transaction linked to multiple booklets`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val secondBooklet = Booklet(id = UUID.randomUUID(), amount = 1000.toAmount(), label = "Compte épargne", owner = ctx.user)
            factory.bookletState().initWith(
                BookletsByOwner(listOf(ctx.booklet, secondBooklet), ctx.userId)
            )

            val monthlyTransaction = RegularTransaction(
                label = "Épargne automatique", amount = 300.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId}-monthly-savings"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val result = bookRegularTransactionUseCase.handle(BookRegularTransactionCommand(ctx.userId, monthlyTransaction, listOf(ctx.booklet.id!!, secondBooklet.id!!)))

            result.assertSuccess()
            result.onSuccess { savedTransaction -> assertNotNull(savedTransaction.id) }
        }
    }

    @Nested
    inner class GetRegularTransactionByIdTest {

        @Test
        fun `should retrieve regular transaction by id`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val monthlyTransaction = RegularTransaction(
                label = "Abonnement Spotify", amount = 9.99.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId}-monthly-spotify"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
            )
            regularTransactionState.init(listOf(UserRegularTransaction(ctx.userId, monthlyTransaction)))

            val result = getRegularTransactionByIdUseCase.handle(GetRegularTransactionByIdQuery(ctx.userId, monthlyTransaction.id.value))

            result.assertSuccess()
            result.onSuccess { transaction ->
                assertEquals("Abonnement Spotify", transaction.label)
                assertEquals(9.99.toAmount(), transaction.amount)
                assertEquals(monthlyTransaction.id, transaction.id)
            }
        }

        @Test
        fun `should fail when transaction id does not exist`() {
            val ctx = given { scenario.withUser().withBooklet() }

            val result = getRegularTransactionByIdUseCase.handle(GetRegularTransactionByIdQuery(ctx.userId, "non-existing-id"))

            result.assertFailure()
            assertEquals("domain.regular_transaction.get_by_id.not_found", result.errorInfo?.key)
        }
    }

    @Nested
    inner class DeleteRegularTransactionTest {

        @Test
        fun `should delete a regular transaction successfully`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val monthlyTransaction = RegularTransaction(
                label = "Abonnement à supprimer", amount = 19.99.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId}-monthly-delete"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
            )
            regularTransactionState.init(listOf(UserRegularTransaction(ctx.userId, monthlyTransaction)))

            val result = deleteRegularTransactionUseCase.handle(DeleteRegularTransactionCommand(ctx.userId, monthlyTransaction.id.value))

            result.assertSuccess()
            result.onSuccess { deleted -> assertTrue(deleted) }

            val getResult = getRegularTransactionByIdUseCase.handle(GetRegularTransactionByIdQuery(ctx.userId, monthlyTransaction.id.value))
            getResult.assertFailure()
        }

        @Test
        fun `should fail when deleting non-existing transaction`() {
            val ctx = given { scenario.withUser().withBooklet() }

            val result = deleteRegularTransactionUseCase.handle(DeleteRegularTransactionCommand(ctx.userId, "non-existing-id"))

            result.assertFailure()
            assertEquals("domain.regular_transaction.delete.not_found", result.errorInfo?.key)
        }

        @Test
        fun `should not delete transaction belonging to another user`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val otherUserId = UserId(UUID.randomUUID())
            val monthlyTransaction = RegularTransaction(
                label = "Transaction d'un autre utilisateur", amount = 25.toAmount(), isIncome = false,
                id = RegularTransactionId("${otherUserId.value}-monthly-other"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
            )
            regularTransactionState.init(listOf(UserRegularTransaction(otherUserId, monthlyTransaction)))

            val result = deleteRegularTransactionUseCase.handle(DeleteRegularTransactionCommand(ctx.userId, monthlyTransaction.id.value))

            result.assertFailure()
            assertEquals("domain.regular_transaction.delete.not_found", result.errorInfo?.key)
        }

        @Test
        fun `should delete multiple regular transactions successfully`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val first = RegularTransaction(
                label = "Transaction 1", amount = 10.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId}-bulk-1"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
            )
            val second = RegularTransaction(
                label = "Transaction 2", amount = 20.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId}-bulk-2"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(2)
            )
            regularTransactionState.init(listOf(UserRegularTransaction(ctx.userId, first), UserRegularTransaction(ctx.userId, second)))

            val result = deleteRegularTransactionsUseCase.handle(DeleteRegularTransactionsCommand(ctx.userId, listOf(first.id.value, second.id.value)))

            result.assertSuccess()
            result.onSuccess { deletedIds ->
                assertEquals(2, deletedIds.size)
                assertTrue(deletedIds.contains(first.id.value))
                assertTrue(deletedIds.contains(second.id.value))
            }

            val remaining = getAllRegularTransactionsUseCase.handle(GetAllRegularTransactionsQuery(ctx.userId, 0, 10))
            remaining.assertSuccess()
            remaining.onSuccess { all ->
                assertTrue(all.content.none { it.id == first.id })
                assertTrue(all.content.none { it.id == second.id })
            }
        }

        @Test
        fun `should fail bulk deletion when selection is empty`() {
            val ctx = given { scenario.withUser().withBooklet() }

            val result = deleteRegularTransactionsUseCase.handle(DeleteRegularTransactionsCommand(ctx.userId, emptyList()))

            result.assertFailure(ResultState.TRANSACTION_ENTRY_ERROR)
            assertEquals("domain.regular_transaction.delete.bulk.empty_selection", result.errorInfo?.key)
        }

        @Test
        fun `should fail bulk deletion when one transaction is missing`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val first = RegularTransaction(
                label = "Transaction 1", amount = 10.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId}-bulk-missing-1"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
            )
            regularTransactionState.init(listOf(UserRegularTransaction(ctx.userId, first)))

            val result = deleteRegularTransactionsUseCase.handle(DeleteRegularTransactionsCommand(ctx.userId, listOf(first.id.value, "missing-id")))

            result.assertFailure(ResultState.TRANSACTION_NOT_FOUND)
            assertEquals("domain.regular_transaction.delete.bulk.not_found", result.errorInfo?.key)

            val after = getRegularTransactionByIdUseCase.handle(GetRegularTransactionByIdQuery(ctx.userId, first.id.value))
            after.assertSuccess()
        }
    }

    @Nested
    inner class UpdateRegularTransactionTest {
        @Test
        fun `should patch a regular transaction correctly`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val originalTransaction = RegularTransaction(
                label = "Abonnement Gym", amount = 40.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId}-monthly-gym"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
            )
            regularTransactionState.init(listOf(UserRegularTransaction(ctx.userId, originalTransaction)))

            val updatedTransaction = originalTransaction.copy(amount = 45.toAmount(), label = "Abonnement Gym Premium")

            val result = updateRegularTransactionUseCase.handle(UpdateRegularTransactionCommand(ctx.userId, updatedTransaction, listOf(ctx.booklet.id!!)))

            result.assertSuccess()
            result.onSuccess { transaction ->
                assertEquals("Abonnement Gym Premium", transaction.label)
                assertEquals(45.toAmount(), transaction.amount)
                assertEquals(originalTransaction.id, transaction.id)
            }
        }

        @Test
        fun `should fail when updating unknown regular transaction`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val unknownTransaction = RegularTransaction(
                label = "Unknown", amount = 45.toAmount(), isIncome = false,
                id = RegularTransactionId("unknown-id"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val result = updateRegularTransactionUseCase.handle(UpdateRegularTransactionCommand(ctx.userId, unknownTransaction, listOf(ctx.booklet.id!!)))

            result.assertFailure(ResultState.TRANSACTION_NOT_FOUND)
            assertEquals("domain.regular_transaction.update.not_found", result.errorInfo?.key)
        }
    }

    @Nested
    inner class LinkBookletTest {

        @Test
        fun `should link a booklet to a regular transaction successfully`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val transaction = RegularTransaction(
                label = "Salaire", amount = 2500.toAmount(), isIncome = true,
                id = RegularTransactionId("${ctx.userId}-link-1"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
            )
            regularTransactionState.init(listOf(UserRegularTransaction(ctx.userId, transaction)))

            val result = linkRegularTransactionToBookletUseCase.handle(LinkRegularTransactionToBookletCommand(ctx.userId, transaction.id.value, ctx.booklet.id!!))

            result.assertSuccess()
            result.onSuccess { updated ->
                assertTrue(updated.associatedBooklets.any { it.id == ctx.booklet.id })
            }
        }

        @Test
        fun `should fail when linking booklet to a non-existing transaction`() {
            val ctx = given { scenario.withUser().withBooklet() }

            val result = linkRegularTransactionToBookletUseCase.handle(LinkRegularTransactionToBookletCommand(ctx.userId, "non-existing-id", ctx.booklet.id!!))

            result.assertFailure(ResultState.TRANSACTION_NOT_FOUND)
            assertEquals("domain.regular_transaction.link.not_found", result.errorInfo?.key)
        }

        @Test
        fun `should fail when linking an already linked booklet`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val transaction = RegularTransaction(
                label = "Loyer", amount = 700.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId}-link-already"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(5),
                associatedBooklets = listOf(ctx.booklet)
            )
            regularTransactionState.init(listOf(UserRegularTransaction(ctx.userId, transaction, listOf(ctx.booklet.id!!))))

            val result = linkRegularTransactionToBookletUseCase.handle(LinkRegularTransactionToBookletCommand(ctx.userId, transaction.id.value, ctx.booklet.id!!))

            result.assertFailure(ResultState.BAD_REQUEST)
            assertEquals("domain.regular_transaction.link.already_linked", result.errorInfo?.key)
        }
    }

    @Nested
    inner class UnlinkBookletTest {

        @Test
        fun `should unlink a booklet from a regular transaction successfully`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val transaction = RegularTransaction(
                label = "Abonnement streaming", amount = 12.99.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId}-unlink-1"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1),
                associatedBooklets = listOf(ctx.booklet)
            )
            regularTransactionState.init(listOf(UserRegularTransaction(ctx.userId, transaction, listOf(ctx.booklet.id!!))))

            val result = unlinkRegularTransactionFromBookletUseCase.handle(UnlinkRegularTransactionFromBookletCommand(ctx.userId, transaction.id.value, ctx.booklet.id!!))

            result.assertSuccess()
            result.onSuccess { updated ->
                assertTrue(updated.associatedBooklets.none { it.id == ctx.booklet.id })
            }
        }

        @Test
        fun `should fail when unlinking booklet from a non-existing transaction`() {
            val ctx = given { scenario.withUser().withBooklet() }

            val result = unlinkRegularTransactionFromBookletUseCase.handle(UnlinkRegularTransactionFromBookletCommand(ctx.userId, "non-existing-id", ctx.booklet.id!!))

            result.assertFailure(ResultState.TRANSACTION_NOT_FOUND)
            assertEquals("domain.regular_transaction.unlink.not_found", result.errorInfo?.key)
        }

        @Test
        fun `should fail when unlinking a booklet that is not linked`() {
            val ctx = given { scenario.withUser().withBooklet() }
            val transaction = RegularTransaction(
                label = "Dépense non liée", amount = 50.toAmount(), isIncome = false,
                id = RegularTransactionId("${ctx.userId}-unlink-notlinked"),
                startDate = LocalDate.of(2024, 1, 1),
                frequencyProperty = FrequencyProperty.Forever(), recurrenceRule = RecurrenceRule.Monthly(1)
            )
            regularTransactionState.init(listOf(UserRegularTransaction(ctx.userId, transaction)))

            val result = unlinkRegularTransactionFromBookletUseCase.handle(UnlinkRegularTransactionFromBookletCommand(ctx.userId, transaction.id.value, ctx.booklet.id!!))

            result.assertFailure(ResultState.BAD_REQUEST)
            assertEquals("domain.regular_transaction.unlink.not_linked", result.errorInfo?.key)
        }
    }
}
