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
    open val idAccount: Long? = null

    @Column(name = "amount")
    open val amount: Double? = null

    @Column(name = "label")
    open val label: String? = null

    @OneToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        name = "account_sheet",
        joinColumns = [JoinColumn(name = "id_account")],
        inverseJoinColumns = [JoinColumn(name = "id_sheet")]
    )
    open val sheets: MutableList<SheetResource>? = null

}


