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


}