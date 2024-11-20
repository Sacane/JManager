package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*
import java.math.BigDecimal


@Entity
@Table(name = "account")
class AccountResource(
    @Column(name = "amount")
    var amount: BigDecimal = BigDecimal(0.0),
    @Column(name = "label", length = 30, nullable = false)
    var label: String,
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "account")
    var sheets: MutableList<TransactionResource> = mutableListOf(),
    @ManyToMany
    val subscriptions: MutableList<SubscriptionEntity> = mutableListOf(),
    @ManyToOne
    var owner: UserResource? = null,
    var initialSold: BigDecimal = BigDecimal.ZERO,
    var previewAmount: BigDecimal = BigDecimal.ZERO,
    @Id
    @GeneratedValue
    @Column(name = "id_account")
    var idAccount: Long? = null
) {
    fun addTransaction(transaction: TransactionResource) {
        sheets.add(transaction)
        transaction.account = this
    }
}
