package fr.sacane.jmanager.infrastructure.spi.entity

import fr.sacane.jmanager.infrastructure.spi.entity.transaction.MonthlyRegularRegularTransactionEntity
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
    @ManyToOne
    var owner: UserResource? = null,
    var initialSold: BigDecimal = BigDecimal.ZERO,
    var previewAmount: BigDecimal = BigDecimal.ZERO,
    @ManyToMany(mappedBy = "accounts", fetch = FetchType.LAZY)
    var monthlyTransactions: MutableSet<MonthlyRegularRegularTransactionEntity> = mutableSetOf(),
    @Id
    @GeneratedValue
    @Column(name = "id_account")
    var idAccount: Long? = null
) {
    fun addMonthlyTransaction(monthlyTransaction: MonthlyRegularRegularTransactionEntity) {
        monthlyTransactions.add(monthlyTransaction)
        monthlyTransaction.addBooklet(this)
    }
    fun removeRegularTransaction(monthlyTransaction: MonthlyRegularRegularTransactionEntity) {
        monthlyTransactions.removeIf { it.transactionId == monthlyTransaction.transactionId }
        monthlyTransaction.accounts.remove(this)
    }
}
