package fr.sacane.jmanager.infrastructure.postgres.entity

import jakarta.persistence.*
import java.math.BigDecimal


@Entity
@Table(name = "account")
class AccountResource(
    @Column(name = "amount")
    var amount: BigDecimal = BigDecimal(0.0),
    @Column(name = "label", unique = true)
    var label: String = "undefined",
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JoinTable(
        name = "account_sheet",
        joinColumns = [JoinColumn(name = "id_account")],
        inverseJoinColumns = [JoinColumn(name = "id_sheet")]
    )
    var sheets: MutableList<SheetResource> = mutableListOf(),
    @Id
    @GeneratedValue
    @Column(name = "id_account")
    var idAccount: Long? = null
)
