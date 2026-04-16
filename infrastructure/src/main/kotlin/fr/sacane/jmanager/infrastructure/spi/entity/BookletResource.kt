package fr.sacane.jmanager.infrastructure.spi.entity

import fr.sacane.jmanager.infrastructure.spi.entity.transaction.RegularTransactionEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.UUID
import kotlin.collections.toList


@Entity
@Table(name = "booklet")
class BookletResource(
    @Column(name = "amount")
    var amount: BigDecimal = BigDecimal(0.0),
    @Column(name = "label", length = 30, nullable = false)
    var label: String,
    @Column(name = "monthly_period_start_day", nullable = false)
    var monthlyPeriodStartDay: Int = 1,
    @Column(name = "monthly_period_end_day")
    var monthlyPeriodEndDay: Int? = null,
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "booklet")
    var transactions: MutableList<TransactionResource> = mutableListOf(),
    @ManyToOne
    var owner: UserResource? = null,
    var initialSold: BigDecimal = BigDecimal.ZERO,
    @ManyToMany(mappedBy = "booklets", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var regularTransactions: MutableSet<RegularTransactionEntity> = mutableSetOf(),
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_booklet")
    var idBooklet: UUID? = null,
    @Version
    var version: Long = 0
) {
    fun clearAllRegularTransactions() {
        regularTransactions.toList().forEach { transaction ->
            transaction.removeBooklet(this)
        }
        regularTransactions.clear()
    }
}
