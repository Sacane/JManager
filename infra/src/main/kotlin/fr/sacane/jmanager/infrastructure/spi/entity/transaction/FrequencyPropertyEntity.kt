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
import java.time.LocalDate

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "frequency_type")
sealed class FrequencyPropertyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long? = null
)

@Entity
@DiscriminatorValue("FOREVER")
class ForeverEntity : FrequencyPropertyEntity()

@Entity
@DiscriminatorValue("SPECIFIC_REPETITION_TIMES")
class SpecificRepetitionTimesEntity(
    @Column(nullable = false)
    val number: Int
) : FrequencyPropertyEntity()

@Entity
@DiscriminatorValue("UNTIL_DATE")
class UntilDateEntity(
    @Column(nullable = false)
    val date: LocalDate
) : FrequencyPropertyEntity()