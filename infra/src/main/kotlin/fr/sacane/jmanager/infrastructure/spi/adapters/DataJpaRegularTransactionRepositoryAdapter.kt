package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.Frequency
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository
import fr.sacane.jmanager.infrastructure.spi.entity.AbstractTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.RegularTransactionResource
import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import fr.sacane.jmanager.infrastructure.spi.repositories.AccountJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.RegularTransactionJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TagPersonalPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class DataJpaRegularTransactionRepositoryAdapter(
    private val regularTransactionJpaRepository: RegularTransactionJpaRepository,
    private val defaultTagPostgresRepository: DefaultTagPostgresRepository,
    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository,
    private val userPostgresRepository: UserPostgresRepository,
    private val bookletJpaRepository: AccountJpaRepository,
    private val regularTransactionOperatorAdapter: RegularTransactionOperatorAdapter
): RegularTransactionRepository {

    @Transactional
    override fun saveRegularTransaction(
        userId: UserId,
        transaction: RegularTransaction
    ): RegularTransaction {
        val user = userPostgresRepository.findByIdOrNull(userId.value!!)
            ?: throw IllegalArgumentException("User not found")
        return regularTransactionOperatorAdapter.save(user, transaction).toDomain()
    }


    @Transactional
    override fun saveRegularTransaction(
        userId: UserId,
        startDate: LocalDate,
        label: String,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag,
        frequency: Frequency
    ): RegularTransaction {
        val user = userPostgresRepository.findByIdOrNull(userId.value!!)
            ?: throw IllegalArgumentException("User not found")
        val transaction = RegularTransactionResource(
            startDate = startDate,
            label = label,
            amount = amount.amount,
            isIncome = isIncome,
            frequency = frequency
        )
        transaction.addOwner(user)

        val toInsertTransaction = when(val tagResource = tag.mapToTag()) {
            is DefaultTagResource -> transaction.copy(
                tag = tagResource,
                personalTag = null
            )
            is TagPersonalResource -> transaction.copy(
                tag = null,
                personalTag = tagResource
            )
            null -> transaction.copy(
                tag = defaultTagPostgresRepository.findUnknownTag(),
                personalTag = null
            )
        }
        val savedRegularTransactionResource = regularTransactionJpaRepository.save(toInsertTransaction)
        return savedRegularTransactionResource.toDomain()
    }

    override fun getAllRegularTransactions(userId: UserId): List<RegularTransaction> {
        return regularTransactionJpaRepository.findAllByUserId(userId.value!!)
            .map { it.toDomain() }
    }

    override fun getAllRegularUsedByAccount(userId: UserId, accountID: Long): List<RegularTransaction> {
        return regularTransactionJpaRepository.findAllByUserId(userId.value!!)
            .filter { it.accounts.any { account -> account.idAccount == accountID } }
            .map { it.toDomain() }
    }

    @Transactional
    override fun linkedRegularTransactionsWithBooklet(
        userId: UserId,
        regularTransactionId: RegularTransactionId,
        bookletId: Long
    ) {
        val booklet = bookletJpaRepository.findByIdOrNull(bookletId)
            ?: throw IllegalArgumentException("Booklet not found")

        val regularTransaction = regularTransactionJpaRepository.findByIdOrNull(regularTransactionId.value.asUUID()) ?:
            throw IllegalArgumentException("Regular transaction not found")

        booklet.addRegularTransaction(regularTransaction)
        regularTransactionJpaRepository.save(regularTransaction)
    }


    private fun Tag.mapToTag(): AbstractTagResource? {
        return this.id?.let {
            if(this.isDefault) {
                defaultTagPostgresRepository.findByIdNullable(it)
            } else {
                tagPersonalPostgresRepository.findByIdNullable(it)
            }
        }
    }
}

private fun String.asUUID(): UUID {
    return UUID.fromString(this)
}
