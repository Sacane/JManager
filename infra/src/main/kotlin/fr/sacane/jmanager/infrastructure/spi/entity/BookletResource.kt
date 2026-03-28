package fr.sacane.jmanager.infrastructure.spi.entity

import fr.sacane.jmanager.infrastructure.spi.entity.transaction.RegularTransactionEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.UUID
import kotlin.collections.toList


@Entity
@Table(name = "account")
class BookletResource(
    @Column(name = "amount")
    var amount: BigDecimal = BigDecimal(0.0),
    @Column(name = "label", length = 30, nullable = false)
    var label: String,
    @Column(name = "monthly_period_start_day", nullable = false)
    var monthlyPeriodStartDay: Int = 1,
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "account")
    var sheets: MutableList<TransactionResource> = mutableListOf(),
    @ManyToOne
    var owner: UserResource? = null,
    var initialSold: BigDecimal = BigDecimal.ZERO,
    @ManyToMany(mappedBy = "accounts", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var regularTransactions: MutableSet<RegularTransactionEntity> = mutableSetOf(),
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_account")
    var idAccount: UUID? = null
) {
    fun clearAllRegularTransactions() {
        regularTransactions.toList().forEach { transaction ->
            transaction.removeBooklet(this)
        }
        regularTransactions.clear()
    }
}
