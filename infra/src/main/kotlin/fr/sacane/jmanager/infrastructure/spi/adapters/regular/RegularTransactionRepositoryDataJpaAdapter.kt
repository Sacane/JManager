package fr.sacane.jmanager.infrastructure.spi.adapters.regular

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.MonthlyTransactionResourceJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class RegularTransactionRepositoryDataJpaAdapter(
    private val userPostgresRepository: UserPostgresRepository,
    private val regularTransactionOperator: RegularTransactionOperator,
    private val monthlyRegularTransactionRepository: MonthlyTransactionResourceJpaRepository
): RegularTransactionRepository {
    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(RegularTransactionRepositoryDataJpaAdapter::class.java)
    }

    override fun saveMonthlyRegularTransaction(
        userId: UserId,
        monthlyTransaction: MonthlyTransaction,
        bookletIds: List<Long>
    ): RegularTransaction{
        logger.info("Saving transaction {}", monthlyTransaction)
        val user = userPostgresRepository.findByIdOrNull(userId.value!!)
            ?: throw IllegalArgumentException("User not found")
        return regularTransactionOperator.save(user, monthlyTransaction, bookletIds).toDomain()
    }

    override fun getRegularTransactionById(
        userId: UserId,
        transactionId: RegularTransactionId
    ): RegularTransaction? {
        val id = transactionId.value.asUUID()

        return monthlyRegularTransactionRepository.findByIdOrNull(id)?.toDomain()
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
}

private fun String.asUUID(): UUID {
    return UUID.fromString(this)
}
