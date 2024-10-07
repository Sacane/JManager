package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserAccountID
import java.time.LocalDate


@Port(Side.APPLICATION)
interface PreviewTransactionFeature {
    fun bookPreviewTransaction(
        userAccountID: UserAccountID,
        label: String,
        date: LocalDate,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag = Tag("Aucune", isDefault = true)
    )
}

@DomainService
class PreviewTransactionFeatureImpl : PreviewTransactionFeature {
    override fun bookPreviewTransaction(
        userAccountID: UserAccountID,
        label: String,
        date: LocalDate,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag
    ) {
        TODO("Not yet implemented")
    }

}