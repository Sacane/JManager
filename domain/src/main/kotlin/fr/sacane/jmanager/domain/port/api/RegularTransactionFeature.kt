package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.Regularity
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success
import java.time.LocalDate

@Port(Side.APPLICATION)
sealed interface RegularTransactionFeature {
    fun bookRegularTransaction(
        token: String,
        startDate: LocalDate,
        label: String,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag = Tag("Aucune", isDefault = true),
        regularity: Regularity = Regularity.MONTHLY
    ): Result<RegularTransaction>
}

@DomainService
class RegularTransactionFeatureImpl(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val session: SessionManager
) : RegularTransactionFeature {

    override fun bookRegularTransaction(
        token: String,
        startDate: LocalDate,
        label: String,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag,
        regularity: Regularity
    ): Result<RegularTransaction> = session.authenticate(token) {
        val transaction = regularTransactionRepository.saveRegularTransaction(
            it,
            startDate,
            label,
            amount,
            isIncome,
            tag,
            regularity
        )
        return@authenticate success(transaction)
    }
}