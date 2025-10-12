package fr.sacane.jmanager.infrastructure.spi.adapters.regular

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.AccountJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.MonthlyTransactionResourceJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class RegularTransactionRepositoryDataJpaAdapter(
    private val userPostgresRepository: UserPostgresRepository,
    private val bookletJpaRepository: AccountJpaRepository,
    private val regularTransactionOperatorAdapter: RegularTransactionOperatorAdapter,
    private val monthlyRegularTransactionRepository: MonthlyTransactionResourceJpaRepository
): RegularTransactionRepository {
    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(RegularTransactionRepositoryDataJpaAdapter::class.java)
    }

    @Transactional
    override fun saveRegularTransaction(
        userId: UserId,
        transaction: RegularTransaction,
        bookletIds: List<Long>
    ): RegularTransaction {
        val user = userPostgresRepository.findByIdOrNull(userId.value!!)
            ?: throw IllegalArgumentException("User not found")
        return regularTransactionOperatorAdapter.save(user, transaction, bookletIds).toDomain()
    }

    override fun saveMonthlyRegularTransaction(
        userId: UserId,
        monthlyTransaction: MonthlyTransaction,
        bookletIds: List<Long>
    ): RegularTransaction{
        logger.info("Saving transaction {}", monthlyTransaction)
        val user = userPostgresRepository.findByIdOrNull(userId.value!!)
            ?: throw IllegalArgumentException("User not found")
        return regularTransactionOperatorAdapter.save(user, monthlyTransaction, bookletIds).toDomain()
    }

    override fun getRegularTransactionById(
        userId: UserId,
        transactionId: RegularTransactionId
    ): RegularTransaction {
        val id = transactionId.value.asUUID()

        return monthlyRegularTransactionRepository.findByIdOrNull(id)?.toDomain()
            ?: throw IllegalArgumentException("Transaction $transactionId not found")
    }


    override fun getAllRegularTransactions(userId: UserId): List<RegularTransaction> {
        val monthlyOnes = monthlyRegularTransactionRepository.findAll()
            .filter { it.owner?.idUser == userId.value }

        // TODO later add others regular transaction
        return monthlyOnes.map { it.toDomain() }
    }

    override fun getAllRegularUsedByAccount(userId: UserId, accountID: Long): List<RegularTransaction>? {
        val monthlyOnes = monthlyRegularTransactionRepository.findAll()
            .filter { it.owner?.idUser == userId.value }
            .filter { transaction -> transaction.accounts.any { it.idAccount == accountID } }
        return monthlyOnes.map { it.toDomain() }
    }

    @Transactional
    override fun linkedRegularTransactionsWithBooklet(
        userId: UserId,
        regularTransactionId: RegularTransactionId,
        bookletId: Long
    ) {
        val booklet = bookletJpaRepository.findByIdOrNull(bookletId)
            ?: throw IllegalArgumentException("Booklet not found")

        val regularTransaction = monthlyRegularTransactionRepository.findByIdOrNull(regularTransactionId.value.asUUID()) ?:
            throw IllegalArgumentException("Regular transaction not found")

        booklet.addMonthlyTransaction(regularTransaction)
        monthlyRegularTransactionRepository.save(regularTransaction)
    }
}

private fun String.asUUID(): UUID {
    return UUID.fromString(this)
}
