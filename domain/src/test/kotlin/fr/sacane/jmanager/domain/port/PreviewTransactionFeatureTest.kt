package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.fake.FakeFactory
import fr.sacane.jmanager.domain.port.api.PreviewTransactionFeature

class PreviewTransactionFeatureTest: FeatureTest() {

    private val accountState: State<AccountByOwner> = FakeFactory.accountState()
    private val previewTransactionFeature: PreviewTransactionFeature = FakeFactory.previewTransactionFeature


}