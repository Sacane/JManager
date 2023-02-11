package fr.sacane.jmanager.infrastructure.server.entity

import fr.sacane.jmanager.domain.hexadoc.DatasourceEntity
import javax.persistence.*


@Entity
@Table(name = "account")
@DatasourceEntity
class AccountResource(
    @Id
    @GeneratedValue
    @Column(unique = true, name = "id_account", nullable = false)
    var idAccount: Long? = null,

    @Column(name = "amount")
    var amount: Double? = null,

    @Column(name = "label", unique = true)
    var label: String? = null,


    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JoinTable(
        name = "account_sheet",
        joinColumns = [JoinColumn(name = "id_account")],
        inverseJoinColumns = [JoinColumn(name = "id_sheet")]
    )
    var sheets: MutableList<SheetResource>? = null
){
    constructor(
        amount: Double?,
        label: String?,
        sheets: MutableList<SheetResource>?
    ) : this(null, amount, label, sheets)
}

