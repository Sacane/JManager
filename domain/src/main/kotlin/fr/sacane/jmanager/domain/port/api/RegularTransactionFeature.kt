package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID

@Port(Side.APPLICATION)
/**
 * Application port: RegularTransactionFeature
 *
 * High-level API for managing regular (recurring) transactions exposed to the application layer.
 * Implementations are responsible for authentication and returning domain Result<T>
 * objects that represent success or failure states.
 */
sealed interface RegularTransactionFeature {

    /**
     * Retrieve all regular transactions for the authenticated user.
     *
     * @param token Authentication token identifying the requester.
     * @return Result containing a list of RegularTransaction on success.
     */
    fun getAllRegularTransactions(token: String): Result<List<RegularTransaction>>

    /**
     * Create (book) a new regular transaction and associate it with multiple booklets.
     *
     * @param token Authentication token identifying the requester.
     * @param regularTransaction The RegularTransaction to persist.
     * @param bookletIds List of booklet UUIDs that will be associated with the created regular transaction.
     * @return Result containing the persisted RegularTransaction on success, or an error state.
     */
    fun bookRegularTransaction(
        token: String,
        regularTransaction: RegularTransaction,
        bookletIds: List<UUID>
    ): Result<RegularTransaction>

    /**
     * Retrieve a single regular transaction by its identifier.
     *
     * @param token Authentication token identifying the requester.
     * @param transactionId The identifier of the regular transaction to retrieve.
     * @return Result containing the RegularTransaction on success, or TRANSACTION_NOT_FOUND when missing.
     */
    fun getRegularTransactionById(token: String, transactionId: String): Result<RegularTransaction>

    /**
     * Update an existing regular transaction.
     *
     * @param token Authentication token identifying the requester.
     * @param regularTransaction RegularTransaction object containing updated values (must include id).
     * @return Result containing the updated RegularTransaction on success, or an error state if not found.
     */
    fun updateRegularTransaction(token: String, regularTransaction: RegularTransaction): Result<RegularTransaction>

    /**
     * Delete a regular transaction by its identifier.
     *
     * @param token Authentication token identifying the requester.
     * @param transactionId Identifier of the regular transaction to delete.
     * @return Result containing a boolean indicating deletion success, or a failure when not found.
     */
    fun deleteRegularTransaction(token: String, transactionId: String): Result<Boolean>
}

@DomainService
class RegularTransactionFeatureImpl(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val tagRepository: TagRepository,
    private val session: SessionManager,
    private val unitOfWork: UnitOfWorkTransactionProvider
) : RegularTransactionFeature {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }


    override fun getAllRegularTransactions(token: String): Result<List<RegularTransaction>> {
        return session.authenticate(token) {
            val transactions = regularTransactionRepository.getAllRegularTransactions(it)
            return@authenticate success(transactions)
        }
    }

    override fun bookRegularTransaction(
        token: String,
        regularTransaction: RegularTransaction,
        bookletIds: List<UUID>
    ): Result<RegularTransaction> = session.authenticate(token = token){ userId ->
        return@authenticate unitOfWork.executeInTransaction(
            regularTransaction
        ) {
            val transactionWithId = it.copy(id = RegularTransactionId(UUID.randomUUID().toString()))
            val transaction = regularTransactionRepository.saveRegularTransaction(
                userId = userId,
                regularTransaction = transactionWithId,
                bookletIds = bookletIds
            )
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
        ) ?: return@authenticate domainFailure(
            ResultState.TRANSACTION_NOT_FOUND,
            "La transaction $transactionId n'existe pas",
            "domain.regular_transaction.get_by_id.not_found"
        )
        return@authenticate success(result)
    }

    override fun updateRegularTransaction(
        token: String,
        regularTransaction: RegularTransaction
    ): Result<RegularTransaction> = session.authenticate(token) { userId ->
        return@authenticate unitOfWork.executeInTransaction(regularTransaction) {
            val updated = regularTransactionRepository.updateRegularTransaction(userId, it)
                ?: return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction ${it.id} n'existe pas",
                    "domain.regular_transaction.update.not_found"
                )
            return@executeInTransaction success(updated)
        }
    }

    override fun deleteRegularTransaction(
        token: String,
        transactionId: String
    ): Result<Boolean> = session.authenticate(token) { userId ->
        return@authenticate unitOfWork.executeInTransaction(transactionId) {
            val deleted = regularTransactionRepository.deleteRegularTransaction(userId, RegularTransactionId(it))
            if (!deleted) {
                return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction $transactionId n'existe pas",
                    "domain.regular_transaction.delete.not_found"
                )
            }
            return@executeInTransaction success(true)
        }
    }
}