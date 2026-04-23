package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.Paginator
import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Page
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.models.SessionToken
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
     * Retrieve all regular transactions for the authenticated user, paginated.
     *
     * @param token Authentication token identifying the requester.
     * @param pageNumber Zero-based page number (default: 0).
     * @param pageSize Number of items per page (default: 10).
     * @return Result containing a Page of RegularTransaction on success.
     */
    fun getAllRegularTransactions(token: SessionToken, pageNumber: Int = 0, pageSize: Int = 10): Result<Page<RegularTransaction>>

    /**
     * Create (book) a new regular transaction and associate it with multiple booklets.
     *
     * @param token Authentication token identifying the requester.
     * @param regularTransaction The RegularTransaction to persist.
     * @param bookletIds List of booklet UUIDs that will be associated with the created regular transaction.
     * @return Result containing the persisted RegularTransaction on success, or an error state.
     */
    fun bookRegularTransaction(
        token: SessionToken,
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
    fun getRegularTransactionById(token: SessionToken, transactionId: String): Result<RegularTransaction>

    /**
     * Update an existing regular transaction.
     *
     * @param token Authentication token identifying the requester.
     * @param regularTransaction RegularTransaction object containing updated values (must include id).
        * @param bookletIds Booklet identifiers to associate with this regular transaction.
     * @return Result containing the updated RegularTransaction on success, or an error state if not found.
     */
        fun updateRegularTransaction(token: SessionToken, regularTransaction: RegularTransaction, bookletIds: List<UUID>): Result<RegularTransaction>

    /**
     * Delete a regular transaction by its identifier.
     *
     * @param token Authentication token identifying the requester.
     * @param transactionId Identifier of the regular transaction to delete.
     * @return Result containing a boolean indicating deletion success, or a failure when not found.
     */
    fun deleteRegularTransaction(token: SessionToken, transactionId: String): Result<Boolean>

    /**
     * Delete multiple regular transactions in a single operation.
     *
     * @param token Authentication token identifying the requester.
     * @param transactionIds Identifiers of regular transactions to delete.
     * @return Result containing deleted transaction ids, or a failure when any id is missing.
     */
    fun deleteRegularTransactions(token: SessionToken, transactionIds: List<String>): Result<List<String>>

    /**
     * Link a booklet to an existing regular transaction.
     *
     * @param token Authentication token identifying the requester.
     * @param transactionId Identifier of the regular transaction.
     * @param bookletId UUID of the booklet to link.
     * @return Result containing the updated RegularTransaction on success, or an error state.
     */
    fun linkRegularTransactionToBooklet(token: SessionToken, transactionId: String, bookletId: UUID): Result<RegularTransaction>

    /**
     * Unlink a booklet from an existing regular transaction.
     * Removing the link also deletes the generation tracker for that pair,
     * so no more virtual/preview transactions will be generated for this booklet.
     *
     * @param token Authentication token identifying the requester.
     * @param transactionId Identifier of the regular transaction.
     * @param bookletId UUID of the booklet to unlink.
     * @return Result containing the updated RegularTransaction on success, or an error state.
     */
    fun unlinkRegularTransactionFromBooklet(token: SessionToken, transactionId: String, bookletId: UUID): Result<RegularTransaction>
}

@DomainService
class RegularTransactionFeatureImpl(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val tagRepository: TagRepository,
    private val session: SessionManager,
    private val unitOfWork: UnitOfWorkTransactionProvider,
    private val trackerRepository: RegularTransactionTrackerRepository,
    private val paginator: Paginator,
) : RegularTransactionFeature {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }


    override fun getAllRegularTransactions(token: SessionToken, pageNumber: Int, pageSize: Int): Result<Page<RegularTransaction>> {
        return session.authenticate(token) {
            val page = paginator.paginate(pageNumber, pageSize) {
                regularTransactionRepository.getAllRegularTransactions(it)
            }
            return@authenticate success(page)
        }
    }

    override fun bookRegularTransaction(
        token: SessionToken,
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
        token: SessionToken,
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
        token: SessionToken,
        regularTransaction: RegularTransaction,
        bookletIds: List<UUID>
    ): Result<RegularTransaction> = session.authenticate(token) { userId ->
        return@authenticate unitOfWork.executeInTransaction(regularTransaction) {
            val updated = regularTransactionRepository.updateRegularTransaction(userId, it, bookletIds)
                ?: return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction ${it.id} n'existe pas",
                    "domain.regular_transaction.update.not_found"
                )
            return@executeInTransaction success(updated)
        }
    }

    override fun deleteRegularTransaction(
        token: SessionToken,
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

    override fun deleteRegularTransactions(
        token: SessionToken,
        transactionIds: List<String>
    ): Result<List<String>> = session.authenticate(token) { userId ->
        if (transactionIds.isEmpty()) {
            return@authenticate domainFailure(
                ResultState.TRANSACTION_ENTRY_ERROR,
                "Aucune transaction régulière à supprimer",
                "domain.regular_transaction.delete.bulk.empty_selection"
            )
        }

        val distinctIds = transactionIds.distinct()
        return@authenticate unitOfWork.executeInTransaction(distinctIds) { ids ->
            val missingId = ids.firstOrNull {
                regularTransactionRepository.getRegularTransactionById(userId, RegularTransactionId(it)) == null
            }

            if (missingId != null) {
                return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction $missingId n'existe pas",
                    "domain.regular_transaction.delete.bulk.not_found"
                )
            }

            ids.forEach {
                regularTransactionRepository.deleteRegularTransaction(userId, RegularTransactionId(it))
            }

            return@executeInTransaction success(ids)
        }
    }

    override fun linkRegularTransactionToBooklet(
        token: SessionToken,
        transactionId: String,
        bookletId: UUID
    ): Result<RegularTransaction> = session.authenticate(token) { userId ->
        return@authenticate unitOfWork.executeInTransaction(transactionId) {
            val existing = regularTransactionRepository.getRegularTransactionById(userId, RegularTransactionId(it))
                ?: return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction $transactionId n'existe pas",
                    "domain.regular_transaction.link.not_found"
                )
            if (existing.associatedBooklets.any { b -> b.id == bookletId }) {
                return@executeInTransaction domainFailure(
                    ResultState.BAD_REQUEST,
                    "Le livret $bookletId est déjà lié à cette transaction",
                    "domain.regular_transaction.link.already_linked"
                )
            }
            val updated = regularTransactionRepository.linkBooklet(userId, RegularTransactionId(it), bookletId)
                ?: return@executeInTransaction domainFailure(
                    ResultState.NOT_FOUND,
                    "Le livret $bookletId est introuvable",
                    "domain.regular_transaction.link.booklet_not_found"
                )
            return@executeInTransaction success(updated)
        }
    }

    override fun unlinkRegularTransactionFromBooklet(
        token: SessionToken,
        transactionId: String,
        bookletId: UUID
    ): Result<RegularTransaction> = session.authenticate(token) { userId ->
        return@authenticate unitOfWork.executeInTransaction(transactionId) {
            val existing = regularTransactionRepository.getRegularTransactionById(userId, RegularTransactionId(it))
                ?: return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction $transactionId n'existe pas",
                    "domain.regular_transaction.unlink.not_found"
                )
            if (existing.associatedBooklets.none { b -> b.id == bookletId }) {
                return@executeInTransaction domainFailure(
                    ResultState.BAD_REQUEST,
                    "Le livret $bookletId n'est pas lié à cette transaction",
                    "domain.regular_transaction.unlink.not_linked"
                )
            }
            val updated = regularTransactionRepository.unlinkBooklet(userId, RegularTransactionId(it), bookletId)
                ?: return@executeInTransaction domainFailure(
                    ResultState.NOT_FOUND,
                    "Le livret $bookletId est introuvable",
                    "domain.regular_transaction.unlink.booklet_not_found"
                )
            // Delete the tracker so no virtual transactions are generated for this pair anymore.
            trackerRepository.deleteTrackerByPair(RegularTransactionId(it), bookletId)
            return@executeInTransaction success(updated)
        }
    }
}