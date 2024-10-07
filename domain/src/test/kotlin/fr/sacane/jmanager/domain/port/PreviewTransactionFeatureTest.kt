package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.models.UserAccountID
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.api.PreviewTransactionFeature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class PreviewTransactionFeatureTest: FeatureTest() {

    private val accountState: State<AccountByOwner> = FakeFactory.accountState()
    private val previewTransactionFeature: PreviewTransactionFeature = FakeFactory.previewTransactionFeature

    @Nested
    inner class BookingPreviewTransaction {

        @Test
        fun `booking a preview transaction should not change the real amount of an account`() {
            launchWithConnectedUserInstance {
                previewTransactionFeature.bookPreviewTransaction(UserAccountID(userId, account.id!!, session), "test#0", "01/01/2024".toDate(), 100.toAmount(), true)
                    .assertSuccess()
                val actualAccount = accountState.getStates().find { it.userId == userId }?.account?.find { it.id == account.id }
                val actualAmount = actualAccount?.amount ?: 10.toAmount().negate()
                val actualPreviewAmount = actualAccount?.previewAmount ?: 0.toAmount()

                assertAll(
                    {assertEquals(0.toAmount(), actualAmount)},
                    {assertEquals(100.toAmount(), actualPreviewAmount)}
                )
            }
        }
    }
}