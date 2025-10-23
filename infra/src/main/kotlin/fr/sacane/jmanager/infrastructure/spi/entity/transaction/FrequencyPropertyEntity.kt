package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.OneToOne
import java.time.LocalDate

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "frequency_type")
abstract class FrequencyPropertyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long? = null,
    @OneToOne(mappedBy = "frequencyProperty", orphanRemoval = true)
    open val regularTransaction: RegularTransactionEntity? = null
) {
    abstract fun toDomain(): FrequencyProperty

    companion object {
        fun fromDomain(frequencyProperty: FrequencyProperty): FrequencyPropertyEntity {
            return when (frequencyProperty) {
                is FrequencyProperty.Forever -> ForeverEntity()
                is FrequencyProperty.SpecificRepetitionTimes -> SpecificRepetitionTimesEntity(frequencyProperty.number)
                is FrequencyProperty.UntilDate -> UntilDateEntity(frequencyProperty.date)
            }
        }
    }
}

@Entity
@DiscriminatorValue("FOREVER")
class ForeverEntity() : FrequencyPropertyEntity() {
    override fun toDomain(): FrequencyProperty = FrequencyProperty.Forever()
}

@Entity
@DiscriminatorValue("SPECIFIC_REPETITION_TIMES")
class SpecificRepetitionTimesEntity(
    @Column
    val number: Int? = null,
) : FrequencyPropertyEntity() {
    override fun toDomain(): FrequencyProperty = FrequencyProperty.SpecificRepetitionTimes(number!!)
}

@Entity
@DiscriminatorValue("UNTIL_DATE")
class UntilDateEntity(
    @Column
    val date: LocalDate? = null,
) : FrequencyPropertyEntity() {
    override fun toDomain(): FrequencyProperty = FrequencyProperty.UntilDate(date!!)
}

