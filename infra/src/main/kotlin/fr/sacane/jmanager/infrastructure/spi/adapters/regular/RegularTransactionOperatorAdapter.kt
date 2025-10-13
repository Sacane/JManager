package fr.sacane.jmanager.infrastructure.spi.adapters.regular

import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.infrastructure.spi.adapters.JpaTagMapperAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.toResource
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.AbstractRegularTransactionResource
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.MonthlyRegularTransactionEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.AccountJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.MonthlyTransactionResourceJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component


@Component
class RegularTransactionOperatorAdapter(
    private val monthlyTransactionResourceJpaRepository: MonthlyTransactionResourceJpaRepository,
    private val tagMapperAdapter: JpaTagMapperAdapter,
    private val defaultTagPostgresRepository: DefaultTagPostgresRepository,
    private val accountJpaRepository: AccountJpaRepository
) {

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(RegularTransactionOperatorAdapter::class.java)
    }

    fun save(user: UserResource, regularTransaction: RegularTransaction, bookletIds: List<Long>): AbstractRegularTransactionResource {
        return when (regularTransaction) {
            is MonthlyTransaction -> {
                val monthlyRegularTransactionEntity = MonthlyRegularTransactionEntity(
                    startDate = regularTransaction.startDate,
                    label = regularTransaction.label,
                    amount = regularTransaction.amount.amount.toDouble(),
                    isIncome = regularTransaction.isIncome,
                    repeatDay = regularTransaction.monthlyRepeatProperty?.repeatDay,
                    frequencyProperty = regularTransaction.frequencyProperty.toResource()
                ).copy(owner = user)
                val result = when(val tagResource = tagMapperAdapter.mapToResource(
                    regularTransaction.tag
                )) {
                    is DefaultTagResource -> monthlyRegularTransactionEntity.copy(
                        tag = tagResource,
                        personalTag = null
                    )
                    is TagPersonalResource -> monthlyRegularTransactionEntity.copy(
                        tag = null,
                        personalTag = tagResource
                    )
                    null -> monthlyRegularTransactionEntity.copy(
                        tag = defaultTagPostgresRepository.findUnknownTag(),
                        personalTag = null
                    )
                }
                bookletIds.forEach { bookletId ->
                    val booklet = accountJpaRepository.findByIdOrNull(bookletId)
                        ?: throw IllegalArgumentException("Booklet with id $bookletId not found")
                    result.addBooklet(booklet)
                }
                logger.info("Save monthly transaction in postgres database {}", result)
                monthlyTransactionResourceJpaRepository.save(result)
            }
        }
    }
}