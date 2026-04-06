package fr.sacane.jmanager.infrastructure.spi.adapters.regular

import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.infrastructure.api.NotFoundException
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.JpaTagMapperAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toDomain
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toResource
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.RecurrenceRuleEntity
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.RegularTransactionEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.BookletJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.RegularTransactionResourceJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component


@Component
class RegularTransactionOperator(
    private val regularTransactionResourceJpaRepository: RegularTransactionResourceJpaRepository,
    private val tagMapperAdapter: JpaTagMapperAdapter,
    private val defaultTagPostgresRepository: DefaultTagPostgresRepository,
    private val bookletJpaRepository: BookletJpaRepository,
    private val defaultTagRepository: DefaultTagPostgresRepository
) {

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(RegularTransactionOperator::class.java)
    }

    @Transactional
    fun save(user: UserResource, regularTransaction: RegularTransaction, bookletIds: List<java.util.UUID>): RegularTransactionEntity {
        val regularTransactionEntity = RegularTransactionEntity(
            startDate = regularTransaction.startDate,
            label = regularTransaction.label,
            amount = regularTransaction.amount.value.toDouble(),
            isIncome = regularTransaction.isIncome,
            frequencyProperty = regularTransaction.frequencyProperty.toResource(),
            recurrenceRule = RecurrenceRuleEntity.fromDomain(regularTransaction.recurrenceRule),
            owner = user
        )

        val result = when(val tagResource = tagMapperAdapter.mapToResource(
            regularTransaction.tag ?: defaultTagRepository.findUnknownTag()?.toDomain()!!
        )) {
            is DefaultTagResource -> regularTransactionEntity.copy(
                tag = tagResource,
                personalTag = null
            )
            is TagPersonalResource -> regularTransactionEntity.copy(
                tag = null,
                personalTag = tagResource
            )
            null -> regularTransactionEntity.copy(
                tag = defaultTagPostgresRepository.findUnknownTag(),
                personalTag = null
            )
        }

        bookletIds.forEach { bookletId ->
            val booklet = bookletJpaRepository.findByIdWithRegularTransactions(bookletId)
                ?: throw NotFoundException(ResultState.NOT_FOUND.code,"Booklet with id $bookletId not found")
            result.addBooklet(booklet)
        }

        logger.info("Save regular transaction in postgres database {}", result)
        return regularTransactionResourceJpaRepository.save(result)
    }

    fun update(
        existing: RegularTransactionEntity,
        regularTransaction: RegularTransaction,
        bookletIds: List<java.util.UUID>,
        userId: UserId
    ): RegularTransactionEntity {
        existing.label = regularTransaction.label
        existing.amount = regularTransaction.amount.value.toDouble()
        existing.isIncome = regularTransaction.isIncome
        existing.startDate = regularTransaction.startDate
        existing.frequencyProperty = regularTransaction.frequencyProperty.toResource()
        existing.recurrenceRule = RecurrenceRuleEntity.fromDomain(regularTransaction.recurrenceRule)

        when (val tagResource = tagMapperAdapter.mapToResource(
            regularTransaction.tag ?: defaultTagRepository.findUnknownTag()?.toDomain()!!
        )) {
            is DefaultTagResource -> {
                existing.tag = tagResource
                existing.personalTag = null
            }
            is TagPersonalResource -> {
                existing.tag = null
                existing.personalTag = tagResource
            }
            null -> {
                existing.tag = defaultTagPostgresRepository.findUnknownTag()
                existing.personalTag = null
            }
        }

        existing.booklets.toList().forEach { booklet ->
            existing.removeBooklet(booklet)
        }

        bookletIds.distinct().forEach { bookletId ->
            val booklet = bookletJpaRepository.findByIdWithRegularTransactions(bookletId)
                ?: throw NotFoundException(ResultState.NOT_FOUND.code, "Booklet with id $bookletId not found")

            if (booklet.owner?.idUser != userId.value) {
                throw NotFoundException(ResultState.NOT_FOUND.code, "Booklet with id $bookletId not found")
            }

            existing.addBooklet(booklet)
        }

        logger.info("Update regular transaction in postgres database {}", existing)
        return regularTransactionResourceJpaRepository.save(existing)
    }

    @Transactional
    fun link(
        existing: RegularTransactionEntity,
        bookletId: java.util.UUID,
        userId: UserId
    ): RegularTransactionEntity {
        val booklet = bookletJpaRepository.findByIdWithRegularTransactions(bookletId)
            ?: throw NotFoundException(ResultState.NOT_FOUND.code, "Booklet with id $bookletId not found")

        if (booklet.owner?.idUser != userId.value) {
            throw NotFoundException(ResultState.NOT_FOUND.code, "Booklet with id $bookletId not found")
        }

        existing.addBooklet(booklet)
        logger.info("Linked booklet {} to regular transaction {}", bookletId, existing.transactionId)
        return regularTransactionResourceJpaRepository.save(existing)
    }

    @Transactional
    fun unlink(
        existing: RegularTransactionEntity,
        bookletId: java.util.UUID
    ): RegularTransactionEntity {
        val booklet = existing.booklets.find { it.idBooklet == bookletId }
        if (booklet != null) {
            existing.removeBooklet(booklet)
        }
        logger.info("Unlinked booklet {} from regular transaction {}", bookletId, existing.transactionId)
        return regularTransactionResourceJpaRepository.save(existing)
    }
}