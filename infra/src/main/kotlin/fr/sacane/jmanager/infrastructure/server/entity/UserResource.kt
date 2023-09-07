package fr.sacane.jmanager.infrastructure.server.entity

import jakarta.persistence.*
import org.springframework.lang.Nullable


@Table(name="userResource")
@Entity
class UserResource(
    @Id
    @Nullable
    @GeneratedValue
    @Column(name = "id_user", nullable = false)
    var idUser: Long? = null,

    @Column(unique = true)
    var username: String? = null,

    @Column
    var password: ByteArray? = null,

    @Column(unique = true, nullable = true)
    var email: String? = null,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_account",
        joinColumns = [JoinColumn(name = "id_user")],
        inverseJoinColumns = [JoinColumn(name = "idAccount")]
    )
    var accounts: MutableList<AccountResource>?,

    @OneToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        name="user_categories",
        joinColumns = [JoinColumn(name="id_user")],
        inverseJoinColumns = [JoinColumn(name="id_category")]
    )
    var categories: MutableList<CategoryResource>?
){
    constructor() : this(null, null, null, null, null, null)
    constructor(
        username: String?,
        password: ByteArray?,
        email: String?,
        accounts: MutableList<AccountResource>?,
        categories: MutableList<CategoryResource>?

    ) : this(null, username, password, email, accounts, categories)
}
