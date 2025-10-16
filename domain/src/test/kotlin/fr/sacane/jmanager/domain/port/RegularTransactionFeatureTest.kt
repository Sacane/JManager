package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.assertFailure
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.fake.UserRegularTransaction
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyRepeatProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.utils.ResultState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

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
                val monthlyTransaction1 = MonthlyTransaction(
                    label = "monthly salary",
                    amount = 2500.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user.id}-monthly-1"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
                )

                val monthlyTransaction2 = MonthlyTransaction(
                    label = "Rent",
                    amount = 800.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id}-monthly-2"),
                    startDate = LocalDate.of(2024, 1, 5),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(5)
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
                val monthlyTransaction = MonthlyTransaction(
                    label = "Abonnement Netflix",
                    amount = 15.99.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id}-monthly-netflix"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
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
                val monthlyTransaction = MonthlyTransaction(
                    label = "Prêt temporaire",
                    amount = 200.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user.id}-monthly-loan"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.UntilDate(endDate),
                    monthlyRepeatProperty = MonthlyRepeatProperty(15)
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
                val monthlyTransaction = MonthlyTransaction(
                    label = "Cours de yoga - 10 séances",
                    amount = 50.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user}-monthly-yoga"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.SpecificRepetitionTimes(10),
                    monthlyRepeatProperty = MonthlyRepeatProperty(10)
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

                val monthlyTransaction = MonthlyTransaction(
                    label = "Épargne automatique",
                    amount = 300.toAmount(),
                    isIncome = true,
                    id = RegularTransactionId("${user}-monthly-savings"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
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
            val monthlyTransaction = MonthlyTransaction(
                label = "Test",
                amount = 100.toAmount(),
                isIncome = true,
                id = RegularTransactionId("test-id"),
                startDate = LocalDate.now(),
                frequencyProperty = FrequencyProperty.Forever()
            )

            val result = regularTransactionFeature.bookRegularTransaction(
                "invalid-token",
                monthlyTransaction,
                listOf(1L)
            )

            result.assertFailure(ResultState.UNAUTHORIZED)
        }
    }

    @Nested
    inner class GetRegularTransactionByIdTest {

        @Test
        fun `should retrieve regular transaction by id`() {
            launchWithConnectedUserInstance {
                val monthlyTransaction = MonthlyTransaction(
                    label = "Abonnement Spotify",
                    amount = 9.99.toAmount(),
                    isIncome = false,
                    id = RegularTransactionId("${user}-monthly-spotify"),
                    startDate = LocalDate.of(2024, 1, 1),
                    frequencyProperty = FrequencyProperty.Forever(),
                    monthlyRepeatProperty = MonthlyRepeatProperty(1)
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
}