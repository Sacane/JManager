package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.UserRegularTransaction
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class RegularTransactionFeatureTest : FeatureTest() {

    companion object {
        private val regularTransactionFeature = FakeFactory.regularTransactionFeature
        private val regularTransactionState = FakeFactory.regularTransactionState
    }

    @Nested
    inner class GetAllRegularTransactionsTest {

        @Test
        fun `should retrieve all regular transactions for authenticated user`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction1 = RegularTransaction(
                    label = "monthly salary",
                    amount = 2500.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id}-monthly-1"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                val monthlyTransaction2 = RegularTransaction(
                    label = "Rent",
                    amount = 800.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id}-monthly-2"),
                    startDate = LocalDate.of(2024, 1, 5),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(5)
                )

                regularTransactionState.init(listOf(UserRegularTransaction(user.id, monthlyTransaction1), UserRegularTransaction(user.id, monthlyTransaction2)))

                val result = regularTransactionFeature.getAllRegularTransactions(tokenValue)

                result.assertSuccess()
                result.onSuccess { transactions ->
                    assertEquals(2, transactions.size)
                    assertTrue(transactions.any { it.label == "monthly salary" })
                    assertTrue(transactions.any { it.label == "Rent" })
                }
            }
        }

        @Test
        fun `should return empty list when user has no regular transactions`() {
            launchWithConnectedUserInstance {
                val result = regularTransactionFeature.getAllRegularTransactions(tokenValue)

                result.assertSuccess()
                result.onSuccess { transactions ->
                    assertTrue(transactions.isEmpty())
                }
            }
        }

        @Test
        fun `should fail with unauthorized when token is invalid`() {
            val result = regularTransactionFeature.getAllRegularTransactions("invalid-token")
            result.assertFailure(ResultState.UNAUTHORIZED)
        }
    }

    @Nested
    inner class BookRegularTransactionTest {

        @Test
        fun `should book a monthly transaction with Forever frequency`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Abonnement Netflix",
                    amount = 15.99.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id}-monthly-netflix"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                val result = regularTransactionFeature.bookRegularTransaction(
                    tokenValue,
                    monthlyTransaction,
                    listOf(booklet.id!!)
                )

                result.assertSuccess()
                result.onSuccess { savedTransaction ->
                    assertEquals("Abonnement Netflix", savedTransaction.label)
                    assertEquals(15.99.toAmount(), savedTransaction.amount)
                    assertFalse(savedTransaction.isIncome)
                    assertNotNull(savedTransaction.id)
                }
            }
        }

        @Test
        fun `should book a monthly transaction with UntilDate frequency`() {
            launchWithConnectedUserInstance {
                val endDate = LocalDate.of(2024, 12, 31)
                val monthlyTransaction = RegularTransaction(
                    label = "Prêt temporaire",
                    amount = 200.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id}-monthly-loan"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.UntilDate(endDate),
                    recurrenceRule = RecurrenceRule.Monthly(15)
                )

                val result = regularTransactionFeature.bookRegularTransaction(
                    tokenValue,
                    monthlyTransaction,
                    listOf(booklet.id!!)
                )

                result.assertSuccess()
                result.onSuccess { savedTransaction ->
                    assertEquals("Prêt temporaire", savedTransaction.label)
                    assertTrue(savedTransaction.frequencyProperty is FrequencyProperty.UntilDate)
                    assertEquals(endDate, (savedTransaction.frequencyProperty as FrequencyProperty.UntilDate).date)
                }
            }
        }

        @Test
        fun `should book a monthly transaction with SpecificRepetitionTimes frequency`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Cours de yoga - 10 séances",
                    amount = 50.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user}-monthly-yoga"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(10),
                    recurrenceRule = RecurrenceRule.Monthly(10)
                )

                val result = regularTransactionFeature.bookRegularTransaction(
                    tokenValue,
                    monthlyTransaction,
                    listOf(booklet.id!!)
                )

                result.assertSuccess()
                result.onSuccess { savedTransaction ->
                    assertEquals("Cours de yoga - 10 séances", savedTransaction.label)
                    assertTrue(savedTransaction.frequencyProperty is FrequencyProperty.SpecificRepetitionTimes)
                    assertEquals(10, (savedTransaction.frequencyProperty as FrequencyProperty.SpecificRepetitionTimes).number)
                }
            }
        }

        @Test
        fun `should book a monthly transaction linked to multiple booklets`() {
            launchWithConnectedUserInstance {
                val secondBooklet = createAccount(user.toUser(), "Compte épargne", 1000.toAmount())

                val monthlyTransaction = RegularTransaction(
                    label = "Épargne automatique",
                    amount = 300.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user}-monthly-savings"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                val result = regularTransactionFeature.bookRegularTransaction(
                    tokenValue,
                    monthlyTransaction,
                    listOf(booklet.id!!, secondBooklet.id!!)
                )

                result.assertSuccess()
                result.onSuccess { savedTransaction ->
                    assertNotNull(savedTransaction.id)
                }
            }
        }

        @Test
        fun `should fail with unauthorized when token is invalid`() {
            val monthlyTransaction = RegularTransaction(
                label = "Test",
                amount = 100.toAmount(),
                isIncome = true,
                id = RegularTransactionId("test-id"),
                startDate = LocalDate.now(),
                frequencyProperty = FrequencyProperty.Forever(),
                recurrenceRule = RecurrenceRule.Monthly(1)
            )

            val result = regularTransactionFeature.bookRegularTransaction(
                "invalid-token",
                monthlyTransaction,
                listOf(UUID.randomUUID())
            )

            result.assertFailure(ResultState.UNAUTHORIZED)
        }
    }

    @Nested
    inner class GetRegularTransactionByIdTest {

        @Test
        fun `should retrieve regular transaction by id`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Abonnement Spotify",
                    amount = 9.99.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user}-monthly-spotify"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                regularTransactionState.init(listOf(UserRegularTransaction(user.id, monthlyTransaction)))

                val result = regularTransactionFeature.getRegularTransactionById(
                    tokenValue,
                    monthlyTransaction.id.value
                )

                result.assertSuccess()
                result.onSuccess { transaction ->
                    assertEquals("Abonnement Spotify", transaction.label)
                    assertEquals(9.99.toAmount(), transaction.amount)
                    assertEquals(monthlyTransaction.id, transaction.id)
                }
            }
        }

        @Test
        fun `should fail when transaction id does not exist`() {
            launchWithConnectedUserInstance {
                val result = regularTransactionFeature.getRegularTransactionById(
                    tokenValue,
                    "non-existing-id"
                )

                result.assertFailure()
                assertEquals("domain.regular_transaction.get_by_id.not_found", result.errorInfo?.key)
            }
        }

        @Test
        fun `should fail with unauthorized when token is invalid`() {
            val result = regularTransactionFeature.getRegularTransactionById(
                "invalid-token",
                "some-id"
            )

            result.assertFailure(ResultState.UNAUTHORIZED)
        }

    }

    @Nested
    inner class DeleteRegularTransactionTest {

        @Test
        fun `should delete a regular transaction successfully`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = RegularTransaction(
                    label = "Abonnement à supprimer",
                    amount = 19.99.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id}-monthly-delete"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                regularTransactionState.init(listOf(UserRegularTransaction(user.id, monthlyTransaction)))

                val result = regularTransactionFeature.deleteRegularTransaction(
                    tokenValue,
                    monthlyTransaction.id.value
                )

                result.assertSuccess()
                result.onSuccess { deleted ->
                    assertTrue(deleted)
                }

                val getResult = regularTransactionFeature.getRegularTransactionById(
                    tokenValue,
                    monthlyTransaction.id.value
                )
                getResult.assertFailure()
            }
        }

        @Test
        fun `should fail when deleting non-existing transaction`() {
            launchWithConnectedUserInstance {
                val result = regularTransactionFeature.deleteRegularTransaction(
                    tokenValue,
                    "non-existing-id"
                )

                result.assertFailure()
                assertEquals("domain.regular_transaction.delete.not_found", result.errorInfo?.key)
            }
        }

        @Test
        fun `should fail with unauthorized when token is invalid`() {
            val result = regularTransactionFeature.deleteRegularTransaction(
                "invalid-token",
                "some-id"
            )

            result.assertFailure(ResultState.UNAUTHORIZED)
        }

        @Test
        fun `should not delete transaction belonging to another user`() {
            launchWithConnectedUserInstance {
                val otherUserId = UserId(UUID.randomUUID())
                val monthlyTransaction = RegularTransaction(
                    label = "Transaction d'un autre utilisateur",
                    amount = 25.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${otherUserId.value}-monthly-other"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                regularTransactionState.init(listOf(UserRegularTransaction(otherUserId, monthlyTransaction)))

                val result = regularTransactionFeature.deleteRegularTransaction(
                    tokenValue,
                    monthlyTransaction.id.value
                )

                result.assertFailure()
                assertEquals("domain.regular_transaction.delete.not_found", result.errorInfo?.key)
            }
        }

        @Test
        fun `should delete multiple regular transactions successfully`() {
            launchWithConnectedUserInstance {
                val first = RegularTransaction(
                    label = "Transaction 1",
                    amount = 10.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id}-bulk-1"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )
                val second = RegularTransaction(
                    label = "Transaction 2",
                    amount = 20.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id}-bulk-2"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(2)
                )

                regularTransactionState.init(
                    listOf(
                        UserRegularTransaction(user.id, first),
                        UserRegularTransaction(user.id, second)
                    )
                )

                val result = regularTransactionFeature.deleteRegularTransactions(
                    tokenValue,
                    listOf(first.id.value, second.id.value)
                )

                result.assertSuccess()
                result.onSuccess { deletedIds ->
                    assertEquals(2, deletedIds.size)
                    assertTrue(deletedIds.contains(first.id.value))
                    assertTrue(deletedIds.contains(second.id.value))
                }

                val remaining = regularTransactionFeature.getAllRegularTransactions(tokenValue)
                remaining.assertSuccess()
                remaining.onSuccess { all ->
                    assertTrue(all.none { it.id == first.id })
                    assertTrue(all.none { it.id == second.id })
                }
            }
        }

        @Test
        fun `should fail bulk deletion when selection is empty`() {
            launchWithConnectedUserInstance {
                val result = regularTransactionFeature.deleteRegularTransactions(tokenValue, emptyList())

                result.assertFailure(ResultState.TRANSACTION_ENTRY_ERROR)
                assertEquals("domain.regular_transaction.delete.bulk.empty_selection", result.errorInfo?.key)
            }
        }

        @Test
        fun `should fail bulk deletion when one transaction is missing`() {
            launchWithConnectedUserInstance {
                val first = RegularTransaction(
                    label = "Transaction 1",
                    amount = 10.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id}-bulk-missing-1"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                regularTransactionState.init(listOf(UserRegularTransaction(user.id, first)))

                val result = regularTransactionFeature.deleteRegularTransactions(
                    tokenValue,
                    listOf(first.id.value, "missing-id")
                )

                result.assertFailure(ResultState.TRANSACTION_NOT_FOUND)
                assertEquals("domain.regular_transaction.delete.bulk.not_found", result.errorInfo?.key)

                val after = regularTransactionFeature.getRegularTransactionById(tokenValue, first.id.value)
                after.assertSuccess()
            }
        }
    }

    @Nested
    inner class UpdateRegularTransactionTest {
        @Test
        fun `should patch a regular transaction correctly`() {
            launchWithConnectedUserInstance {
                val originalTransaction = RegularTransaction(
                    label = "Abonnement Gym",
                    amount = 40.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user}-monthly-gym"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                regularTransactionState.init(listOf(UserRegularTransaction(user.id, originalTransaction)))

                val updatedTransaction = originalTransaction.copy(
                    amount = 45.toAmount(),
                    label = "Abonnement Gym Premium"
                )

                val result = regularTransactionFeature.updateRegularTransaction(
                    tokenValue,
                    updatedTransaction
                )

                result.assertSuccess()
                result.onSuccess { transaction ->
                    assertEquals("Abonnement Gym Premium", transaction.label)
                    assertEquals(45.toAmount(), transaction.amount)
                    assertEquals(originalTransaction.id, transaction.id)
                }
            }
        }

        @Test
        fun `should fail when updating unknown regular transaction`() {
            launchWithConnectedUserInstance {
                val unknownTransaction = RegularTransaction(
                    label = "Unknown",
                    amount = 45.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("unknown-id"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    recurrenceRule = RecurrenceRule.Monthly(1)
                )

                val result = regularTransactionFeature.updateRegularTransaction(tokenValue, unknownTransaction)

                result.assertFailure(ResultState.TRANSACTION_NOT_FOUND)
                assertEquals("domain.regular_transaction.update.not_found", result.errorInfo?.key)
            }
        }
//        }
    }
}