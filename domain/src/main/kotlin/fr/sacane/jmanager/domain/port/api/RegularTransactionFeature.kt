package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.Frequency
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.TagRepository
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
        tag: Tag? = null,
        frequency: Frequency = Frequency.MONTHLY
    ): Result<RegularTransaction>

    fun getAllRegularTransactions(token: String): Result<List<RegularTransaction>>

    fun bookRegularTransaction(
        token: String,
        regularTransaction: RegularTransaction,
    ): Result<RegularTransaction>
}

@DomainService
class RegularTransactionFeatureImpl(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val tagRepository: TagRepository,
    private val session: SessionManager
) : RegularTransactionFeature {

    override fun bookRegularTransaction(
        token: String,
        startDate: LocalDate,
        label: String,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag?,
        frequency: Frequency,
    ): Result<RegularTransaction> = session.authenticate(token) {
        val transaction = regularTransactionRepository.saveRegularTransaction(
            it,
            startDate,
            label,
            amount,
            isIncome,
            tag ?: tagRepository.defaultTag() ?: Tag("Aucune", isDefault = true),
            frequency
        )
        return@authenticate success(transaction)
    }

    override fun getAllRegularTransactions(token: String): Result<List<RegularTransaction>> {
        return session.authenticate(token) {
            val transactions = regularTransactionRepository.getAllRegularTransactions(it)
            return@authenticate success(transactions)
        }
    }

    override fun bookRegularTransaction(
        token: String,
        regularTransaction: RegularTransaction
    ): Result<RegularTransaction>  = session.authenticate(token = token){
        val transaction = regularTransactionRepository.saveRegularTransaction(
            userId = it,
            transaction = regularTransaction
        )
        return@authenticate success(transaction)
    }
}