package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*
import org.springframework.lang.Nullable

@Table(name="userResource")
@Entity
class UserResource(
    @Column(unique = true)
    var username: String = "",
    @Column
    var password: ByteArray = ByteArray(1),
    @Column(unique = true, nullable = true)
    var email: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "owner")
    @JoinTable(
        name = "user_account",
        joinColumns = [JoinColumn(name = "id_user")],
        inverseJoinColumns = [JoinColumn(name = "idAccount")]
    )
    var accounts: MutableList<AccountResource> = mutableListOf(),
    @OneToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        name="user_categories",
        joinColumns = [JoinColumn(name="id_user")],
        inverseJoinColumns = [JoinColumn(name="id_category")]
    )
    var categories: MutableList<CategoryResource> = mutableListOf(),
    @OneToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        name="user_tags",
        joinColumns = [JoinColumn(name="id_user")],
        inverseJoinColumns = [JoinColumn(name="id_tag")]
    )
    var tags: MutableList<TagResource> = mutableListOf(),
    @Id
    @Nullable
    @GeneratedValue
    @Column(name = "id_user", nullable = false)
    var idUser: Long? = null,
)
