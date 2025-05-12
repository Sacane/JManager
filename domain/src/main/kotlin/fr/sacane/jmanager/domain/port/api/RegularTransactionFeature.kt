package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.RegularTransaction
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success


sealed interface RegularTransactionFeature {
    fun bookRegularTransaction(
        token: String,
        transaction: RegularTransaction
    ): Result<RegularTransaction>
}

@DomainService
class RegularTransactionFeatureImpl(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val session: SessionManager
) : RegularTransactionFeature {
    override fun bookRegularTransaction(
        token: String,
        transaction: RegularTransaction
    ): Result<RegularTransaction> = session.authenticate(token) {
        return@authenticate success(regularTransactionRepository.saveRegularTransaction(transaction))
    }
}