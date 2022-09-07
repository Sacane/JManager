package fr.sacane.jmanager.infra.server.entity

import org.springframework.lang.Nullable
import javax.persistence.*


@Table(name="User")
@Entity
class UserResource(
    @Id
    @Nullable
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id_user", nullable = false)
    open var id_user: Long? = null,

    @Column(unique = true, nullable = true)
    var pseudonym: String? = null,

    @Column(unique = true)
    var username: String? = null,
    var password: String? = null,

    @Column(unique = true, nullable = true)
    var email: String? = null,


    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_account",
        joinColumns = [JoinColumn(name = "id_user")],
        inverseJoinColumns = [JoinColumn(name = "idAccount")]
    )
    var accounts: MutableList<AccountResource>?
)
