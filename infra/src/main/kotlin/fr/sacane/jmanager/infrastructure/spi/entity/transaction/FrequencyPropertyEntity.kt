package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.OneToMany
import org.hibernate.internal.util.collections.CollectionHelper.listOf
import java.time.LocalDate

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "frequency_type")
abstract class FrequencyPropertyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long? = null,
    @OneToMany(mappedBy = "frequencyProperty", orphanRemoval = true)
    open val monthlyRegularTransactions: MutableList<MonthlyRegularRegularTransactionEntity> = mutableListOf()
) {
    fun addMonthlyRegularTransaction(monthlyRegularTransaction: MonthlyRegularRegularTransactionEntity) {
        monthlyRegularTransactions.add(monthlyRegularTransaction)
        monthlyRegularTransaction.frequencyProperty = this
    }
}

@Entity
@DiscriminatorValue("FOREVER")
class ForeverEntity : FrequencyPropertyEntity()

@Entity
@DiscriminatorValue("SPECIFIC_REPETITION_TIMES")
class SpecificRepetitionTimesEntity(
    @Column
    val number: Int? = null
) : FrequencyPropertyEntity()

@Entity
@DiscriminatorValue("UNTIL_DATE")
class UntilDateEntity(
    @Column
    val date: LocalDate? = null
) : FrequencyPropertyEntity()