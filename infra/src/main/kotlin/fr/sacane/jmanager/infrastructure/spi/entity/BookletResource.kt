package fr.sacane.jmanager.infrastructure.spi.entity

import fr.sacane.jmanager.infrastructure.spi.entity.transaction.MonthlyRegularTransactionEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.UUID


@Entity
@Table(name = "account")
class BookletResource(
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
    @ManyToMany(mappedBy = "accounts", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var monthlyTransactions: MutableSet<MonthlyRegularTransactionEntity> = mutableSetOf(),
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_account")
    var idAccount: UUID? = null
) {
    fun clearAllMonthlyTransactions() {
        monthlyTransactions.toList().forEach { transaction ->
            transaction.removeBooklet(this)
        }
        monthlyTransactions.clear()
    }
}
