package fr.sacane.jmanager.infra.server.entity

import org.springframework.lang.Nullable
import javax.persistence.*


@Entity
@Table(name = "account")
open class AccountResource{
    @Id
    @Nullable
    @GeneratedValue
    @Column(unique = true, name = "id_account", nullable = false)
    open var idAccount: Long? = null

    @Column(name = "amount")
    open var amount: Double? = null

    @Column(name = "label")
    open var label: String? = null

    @OneToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        name = "account_sheet",
        joinColumns = [JoinColumn(name = "id_account")],
        inverseJoinColumns = [JoinColumn(name = "id_sheet")]
    )
    open var sheets: MutableList<SheetResource>? = null

}


