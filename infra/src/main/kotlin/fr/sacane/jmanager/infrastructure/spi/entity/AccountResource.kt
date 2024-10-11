package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*
import java.math.BigDecimal


@Entity
@Table(name = "account")
class AccountResource(
    @Column(name = "amount")
    var amount: BigDecimal = BigDecimal(0.0),
    @Column(name = "label")
    var label: String = "undefined",
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinTable(
        name = "account_sheet",
        joinColumns = [JoinColumn(name = "id_account")],
        inverseJoinColumns = [JoinColumn(name = "id_sheet")]
    )
    var sheets: MutableList<TransactionResource> = mutableListOf(),
    @ManyToOne
    var owner: UserResource? = null,
    var initialSold: BigDecimal = BigDecimal.ZERO,
    var previewAmount: BigDecimal = BigDecimal.ZERO,
    @Id
    @GeneratedValue
    @Column(name = "id_account")
    var idAccount: Long? = null
)
