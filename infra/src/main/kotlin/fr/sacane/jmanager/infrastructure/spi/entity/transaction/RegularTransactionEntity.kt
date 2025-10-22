package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toDomain
import fr.sacane.jmanager.infrastructure.spi.entity.BookletResource
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "regular_transaction")
data class RegularTransactionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val transactionId: UUID? = null,

    @Column(nullable = false)
    var startDate: LocalDate,

    override var label: String,
    override var amount: Double,
    override var isIncome: Boolean,

    @Column(name = "frequency_property_id")
    @OneToOne(cascade = [CascadeType.ALL])
    override var frequencyProperty: FrequencyPropertyEntity? = null,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "recurrence_rule_id")
    var recurrenceRule: RecurrenceRuleEntity? = null,

    @ManyToOne(cascade = [CascadeType.MERGE, CascadeType.PERSIST])
    override val tag: DefaultTagResource? = null,

    @ManyToOne(cascade = [CascadeType.MERGE, CascadeType.PERSIST])
    override val personalTag: TagPersonalResource? = null,

    @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinTable(
        name = "regular_transaction_booklet",
        joinColumns = [JoinColumn(name = "transaction_id")],
        inverseJoinColumns = [JoinColumn(name = "id_account", nullable = false)]
    )
    var accounts: MutableSet<BookletResource> = mutableSetOf(),

    @ManyToOne(fetch = FetchType.LAZY)
    override val owner: UserResource? = null
) : AbstractRegularTransactionResource(label, amount, isIncome, frequencyProperty) {

    override fun toDomain(): RegularTransaction {
        return RegularTransaction(
            label = this.label,
            amount = Amount(BigDecimal(this.amount)),
            isIncome = this.isIncome,
            id = RegularTransactionId(this.transactionId.toString()),
            startDate = this.startDate,
            tag = this.tag?.toDomain() ?: this.personalTag?.toDomain(),
            frequencyProperty = frequencyProperty!!.toDomain(),
            recurrenceRule = recurrenceRule!!.toDomain()
        )
    }

    fun addBooklet(account: BookletResource) {
        accounts.add(account)
        account.regularTransactions.add(this)
    }

    fun removeBooklet(account: BookletResource) {
        accounts.removeIf { it.idAccount == account.idAccount }
    }

    companion object {
        fun fromDomain(transaction: RegularTransaction, owner: UserResource): RegularTransactionEntity {
            return RegularTransactionEntity(
                transactionId = transaction.id.value.let { UUID.fromString(it) },
                startDate = transaction.startDate,
                label = transaction.label,
                amount = transaction.amount.value.toDouble(),
                isIncome = transaction.isIncome,
                frequencyProperty = FrequencyPropertyEntity.fromDomain(transaction.frequencyProperty),
                recurrenceRule = RecurrenceRuleEntity.fromDomain(transaction.recurrenceRule),
                owner = owner
            )
        }
    }
}

