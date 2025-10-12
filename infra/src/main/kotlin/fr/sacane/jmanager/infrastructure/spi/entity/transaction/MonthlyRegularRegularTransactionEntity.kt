package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyRepeatProperty
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.infrastructure.spi.adapters.toDomain
import fr.sacane.jmanager.infrastructure.spi.entity.AccountResource
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
    @Column(name = "frequency_property_id")
    @OneToOne(cascade = [CascadeType.ALL])
    override var frequencyProperty: FrequencyPropertyEntity? = null,
    val repeatDay: Int?,
    @ManyToOne
    override val tag: DefaultTagResource? = null,
    @ManyToOne
    override val personalTag: TagPersonalResource? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "monthly_transaction_booklet",
        joinColumns = [JoinColumn(name = "transaction_id")],
        inverseJoinColumns = [JoinColumn(name = "id_account")]
    )
    var accounts: MutableSet<AccountResource> = mutableSetOf(),
    @ManyToOne(fetch = FetchType.LAZY)
    override val owner: UserResource? = null
) : AbstractRegularTransactionResource(label, amount, isIncome, frequencyProperty) {

    val getFrequencyProperty: FrequencyProperty get() = frequencyProperty!!.toDomain()

    override fun toDomain(): RegularTransaction {
        return MonthlyTransaction(
            label = this.label,
            amount = Amount(BigDecimal(this.amount)),
            isIncome = this.isIncome,
            id = RegularTransactionId(this.transactionId.toString()),
            this.startDate,
            tag = this.tag?.toDomain() ?: this.personalTag?.toDomain() ?: error("Tag not found in database for transaction with id ${this.transactionId}"),
            frequencyProperty = frequencyProperty!!.toDomain(),
            monthlyRepeatProperty = repeatDay?.let { MonthlyRepeatProperty(it) }
        )
    }
    fun addBooklet(account: AccountResource) {
        accounts.add(account)
        account.monthlyTransactions.add(this)
    }

    fun removeBooklet(account: AccountResource) {
        accounts.remove(account)
        account.monthlyTransactions.remove(this)
    }
}