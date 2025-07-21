package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "monthly_regular_transaction")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "frequency_type")
open class MonthlyRegularTransactionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long? = null,

    @Column(nullable = false)
    open var startDate: LocalDate,

    override var label: String,
    override var amount: Double,
    override var isIncome: Boolean,

) : AbstractTransactionResource(label, amount, isIncome)