package fr.sacane.jmanager.infrastructure.spi.adapters.regular

import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.RegularTransactionResourceJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class RegularTransactionRepositoryDataJpaAdapter(
    private val userPostgresRepository: UserPostgresRepository,
    private val regularTransactionOperator: RegularTransactionOperator,
    private val regularTransactionRepository: RegularTransactionResourceJpaRepository,
    private val regularTransactionTrackerRepository: RegularTransactionTrackerRepository,
    private val defaultTagPostgresRepository: DefaultTagPostgresRepository
): RegularTransactionRepository {
    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(RegularTransactionRepositoryDataJpaAdapter::class.java)
    }

    override fun saveRegularTransaction(
        userId: UserId,
        regularTransaction: RegularTransaction,
        bookletIds: List<UUID>
    ): RegularTransaction {
        logger.info("Saving regular transaction {}", regularTransaction)
        val user = userPostgresRepository.findByIdOrNull(userId.value!!)
            ?: throw IllegalArgumentException("User not found")
        val saved = regularTransactionOperator.save(user, regularTransaction, bookletIds)
        val savedId = saved.transactionId ?: throw IllegalStateException("Saved regular transaction has no id")
        return regularTransactionRepository.findByIdWithBooklets(savedId)?.toDomain()
            ?: throw IllegalStateException("Saved regular transaction $savedId not found")
    }

    override fun getRegularTransactionById(
        userId: UserId,
        transactionId: RegularTransactionId
    ): RegularTransaction? {
        val id = transactionId.value.asUUID()
        return regularTransactionRepository.findByIdWithBooklets(id)?.toDomain()
    }

    override fun getAllRegularTransactions(userId: UserId): List<RegularTransaction> {
        val ownerId = userId.value ?: return emptyList()
        return regularTransactionRepository.findAllByOwnerIdWithBooklets(ownerId)
            .map { it.toDomain() }
    }

    override fun getAllRegularUsedByBooklet(userId: UserId, bookletID: UUID): List<RegularTransaction>? {
        val ownerId = userId.value ?: return emptyList()
        return regularTransactionRepository.findAllByOwnerIdWithBooklets(ownerId)
            .filter { transaction -> transaction.booklets.any { it.idBooklet == bookletID } }
            .map { it.toDomain() }
    }

    @Transactional
    override fun updateRegularTransaction(
        userId: UserId,
        regularTransaction: RegularTransaction,
        bookletIds: List<UUID>
    ): RegularTransaction? {
        val id = regularTransaction.id.value.asUUID()
        val existing = regularTransactionRepository.findByIdOrNull(id) ?: return null

        if (existing.owner?.idUser != userId.value) {
            return null
        }

        regularTransactionOperator.update(existing, regularTransaction, bookletIds, userId)
        return regularTransactionRepository.findByIdWithBooklets(id)?.toDomain()
    }

    @Transactional
    override fun deleteRegularTransaction(
        userId: UserId,
        transactionId: RegularTransactionId
    ): Boolean {
        val id = transactionId.value.asUUID()
        val existing = regularTransactionRepository.findByIdOrNull(id) ?: return false

        if (existing.owner?.idUser != userId.value) {
            return false
        }

        // Remove generation trackers first to avoid orphan tracker state.
        regularTransactionTrackerRepository.deleteTrackerByRegularTransactionId(transactionId)

        // Explicitly detach all booklets before delete to keep the many-to-many link table clean.
        existing.booklets.toList().forEach { booklet ->
            existing.removeBooklet(booklet)
        }

        regularTransactionRepository.delete(existing)
        return true
    }

    @Transactional
    override fun linkBooklet(userId: UserId, transactionId: RegularTransactionId, bookletId: UUID): RegularTransaction? {
        val id = transactionId.value.asUUID()
        val existing = regularTransactionRepository.findByIdOrNull(id) ?: return null

        if (existing.owner?.idUser != userId.value) {
            return null
        }

        regularTransactionOperator.link(existing, bookletId, userId)
        return regularTransactionRepository.findByIdWithBooklets(id)?.toDomain()
    }

    @Transactional
    override fun unlinkBooklet(userId: UserId, transactionId: RegularTransactionId, bookletId: UUID): RegularTransaction? {
        val id = transactionId.value.asUUID()
        val existing = regularTransactionRepository.findByIdOrNull(id) ?: return null

        if (existing.owner?.idUser != userId.value) {
            return null
        }

        regularTransactionOperator.unlink(existing, bookletId)
        return regularTransactionRepository.findByIdWithBooklets(id)?.toDomain()
    }

    @Transactional
    override fun isPersonalTagUsed(tagId: UUID): Boolean {
        return regularTransactionRepository.existsByPersonalTagId(tagId)
    }

    @Transactional
    override fun replacePersonalTagByDefault(tagId: UUID, defaultTag: Tag) {
        val defaultTagResource = defaultTagPostgresRepository.findAll().firstOrNull { it.name == defaultTag.label } ?: return
        regularTransactionRepository.replacePersonalTagByDefaultId(tagId, defaultTagResource.idTag!!)
    }
}
private fun String.asUUID(): UUID {
    return UUID.fromString(this)
}
