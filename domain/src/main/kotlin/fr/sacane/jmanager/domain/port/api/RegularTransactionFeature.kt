package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.Frequency
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.UnitOfWorkTransactionProviderPort
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.TagRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.time.LocalDate
import java.util.UUID

@Port(Side.APPLICATION)
sealed interface RegularTransactionFeature {

    fun getAllRegularTransactions(token: String): Result<List<RegularTransaction>>

    fun bookRegularTransaction(
        token: String,
        regularTransaction: RegularTransaction,
        bookletIds: List<Long>
    ): Result<RegularTransaction>

    fun getRegularTransactionById(token: String, transactionId: String): Result<RegularTransaction>
}

@DomainService
class RegularTransactionFeatureImpl(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val tagRepository: TagRepository,
    private val session: SessionManager,
    private val unitOfWork: UnitOfWorkTransactionProviderPort
) : RegularTransactionFeature {


    override fun getAllRegularTransactions(token: String): Result<List<RegularTransaction>> {
        return session.authenticate(token) {
            val transactions = regularTransactionRepository.getAllRegularTransactions(it)
            return@authenticate success(transactions)
        }
    }

    override fun bookRegularTransaction(
        token: String,
        regularTransaction: RegularTransaction,
        bookletIds: List<Long>
    ): Result<RegularTransaction> = session.authenticate(token = token){ userId ->
        return@authenticate unitOfWork.executeInTransaction(
            regularTransaction
        ) {
            val transaction = when (it) {
                is MonthlyTransaction ->  regularTransactionRepository.saveMonthlyRegularTransaction(
                    userId = userId,
                    monthlyTransaction = it.copy(id = RegularTransactionId(UUID.randomUUID().toString())),
                    bookletIds = bookletIds
                )
            }
            return@executeInTransaction success(transaction)
        }
    }


    override fun getRegularTransactionById(
        token: String,
        transactionId: String
    ): Result<RegularTransaction> = session.authenticate(token) {
        val result = regularTransactionRepository.getRegularTransactionById(
            it,
            RegularTransactionId(transactionId)
        ) ?: return@authenticate failure(ResultState.TRANSACTION_NOT_FOUND, "La transaction $transactionId n'existe pas")
        return@authenticate success(result)
    }
}