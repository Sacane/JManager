package fr.sacane.jmanager.infrastructure.datasource.entity

import jakarta.persistence.*


@Entity
@Table(name = "account")
class AccountResource(
    @Id
    @GeneratedValue
    @Column(name = "id_account")
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
