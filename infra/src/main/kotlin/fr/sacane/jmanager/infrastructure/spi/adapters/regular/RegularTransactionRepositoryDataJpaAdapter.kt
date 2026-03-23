package fr.sacane.jmanager.infrastructure.spi.adapters.regular

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.BookletJpaRepository
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
    private val bookletJpaRepository: BookletJpaRepository,
    private val regularTransactionTrackerRepository: RegularTransactionTrackerRepository
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
        return regularTransactionOperator.save(user, regularTransaction, bookletIds).toDomain()
    }

    override fun getRegularTransactionById(
        userId: UserId,
        transactionId: RegularTransactionId
    ): RegularTransaction? {
        val id = transactionId.value.asUUID()
        return regularTransactionRepository.findByIdWithAccounts(id)?.toDomain()
    }

    override fun getAllRegularTransactions(userId: UserId): List<RegularTransaction> {
        val ownerId = userId.value ?: return emptyList()
        return regularTransactionRepository.findAllByOwnerIdWithAccounts(ownerId)
            .map { it.toDomain() }
    }

    override fun getAllRegularUsedByAccount(userId: UserId, accountID: UUID): List<RegularTransaction>? {
        return bookletJpaRepository.findByIdWithRegularTransactions(accountID)
            ?.regularTransactions?.map { it.toDomain() }
    }

    override fun updateRegularTransaction(
        userId: UserId,
        regularTransaction: RegularTransaction
    ): RegularTransaction? {
        val id = regularTransaction.id.value.asUUID()
        val existing = regularTransactionRepository.findByIdOrNull(id) ?: return null

        if (existing.owner?.idUser != userId.value) {
            return null
        }

        return regularTransactionOperator.update(existing, regularTransaction).toDomain()
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
        existing.accounts.toList().forEach { booklet ->
            existing.removeBooklet(booklet)
        }

        regularTransactionRepository.delete(existing)
        return true
    }
}

private fun String.asUUID(): UUID {
    return UUID.fromString(this)
}
