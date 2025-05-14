package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.Regularity
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository
import fr.sacane.jmanager.infrastructure.spi.entity.AbstractTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.RegularTransactionResource
import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.RegularTransactionJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TagPersonalPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DataJpaRegularTransactionRepositoryAdapter(
    private val regularTransactionJpaRepository: RegularTransactionJpaRepository,
    private val defaultTagPostgresRepository: DefaultTagPostgresRepository,
    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository
): RegularTransactionRepository {
    @Transactional
    override fun saveRegularTransaction(
        userId: UserId,
        startDate: LocalDate,
        label: String,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag,
        regularity: Regularity
    ): RegularTransaction {
        val transaction = RegularTransactionResource(
            startDate = startDate,
            label = label,
            amount = amount.amount,
            isIncome = isIncome,
            regularity = regularity,
        )

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