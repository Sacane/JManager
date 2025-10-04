package fr.sacane.jmanager.infrastructure.spi.adapters.regular

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.infrastructure.spi.adapters.JpaTagMapperAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.toResource
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.AbstractRegularTransactionResource
import fr.sacane.jmanager.infrastructure.spi.entity.transaction.MonthlyRegularRegularTransactionEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.MonthlyTransactionResourceJpaRepository
import org.springframework.stereotype.Component
import java.time.Month


@Component
class RegularTransactionOperatorAdapter(
    private val monthlyTransactionResourceJpaRepository: MonthlyTransactionResourceJpaRepository,
    private val tagMapperAdapter: JpaTagMapperAdapter,
    private val defaultTagPostgresRepository: DefaultTagPostgresRepository
) {

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(RegularTransactionOperatorAdapter::class.java)
    }

    fun save(user: UserResource, regularTransaction: RegularTransaction): AbstractRegularTransactionResource {
        return when (regularTransaction) {
            is MonthlyTransaction -> {
                val frequencyProperty = regularTransaction.frequencyProperty.toResource()
                val monthlyRegularTransactionEntity = MonthlyRegularRegularTransactionEntity(
                    startDate = regularTransaction.startDate,
                    label = regularTransaction.label,
                    amount = regularTransaction.amount.amount.toDouble(),
                    isIncome = regularTransaction.isIncome,
                    repeatDay = regularTransaction.monthlyRepeatProperty?.repeatDay
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
                frequencyProperty.addMonthlyRegularTransaction(result)
                logger.info("Save monthly transaction in postgres database")
                monthlyTransactionResourceJpaRepository.save(result)
            }
        }
    }
}