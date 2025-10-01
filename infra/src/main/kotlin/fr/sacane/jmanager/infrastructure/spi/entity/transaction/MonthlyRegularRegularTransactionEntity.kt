package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.infrastructure.spi.adapters.toDomain
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import jakarta.persistence.*
import java.math.BigDecimal
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
) : AbstractRegularTransactionResource(label, amount, isIncome) {
    override fun toDomain(): RegularTransaction {
        return MonthlyTransaction(
            label = this.label,
            amount = Amount(BigDecimal(this.amount)),
            isIncome = this.isIncome,
            id = RegularTransactionId(this.transactionId.toString()),
            this.startDate,
            tag = this.tag?.toDomain() ?: this.personalTag?.toDomain() ?: error("Tag not found in database for transaction with id ${this.transactionId}"),
            frequencyProperty = this.frequencyProperty?.toDomain()!!
        )
    }
}