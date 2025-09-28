package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "monthly_regular_transaction")
data class MonthlyRegularRegularTransactionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val transactionId: UUID? = null,

    @Column(nullable = false)
    var startDate: LocalDate,

    override var label: String,
    override var amount: Double,
    override var isIncome: Boolean,
    @ManyToOne
    override val tag: DefaultTagResource? = null,
    @ManyToOne
    override val personalTag: TagPersonalResource? = null,
    @ManyToOne(cascade = [CascadeType.ALL])
    var frequencyProperty: FrequencyPropertyEntity? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    override val owner: UserResource? = null
) : AbstractRegularTransactionResource(label, amount, isIncome)