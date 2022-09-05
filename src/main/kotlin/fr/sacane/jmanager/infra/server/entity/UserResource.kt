package fr.sacane.jmanager.infra.server.entity

import org.springframework.lang.Nullable
import javax.persistence.*


@Table(name="User")
@Entity
open class UserResource {
    @Id
    @Nullable
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id_user", nullable = false)
    open var id_user: Int? = null

    @Column(unique = true, nullable = true)
    open var pseudonym: String? = null

    @Column(unique = true)
    open var username: String? = null
    open var password: String? = null

    @Column(unique = true, nullable = true)
    var email: String? = null

    @OneToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        name = "user_account",
        joinColumns = [JoinColumn(name = "id_user")],
        inverseJoinColumns = [JoinColumn(name = "idAccount")]
    )
    var accounts: MutableList<AccountResource>? = null


}
