package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*
import org.springframework.lang.Nullable

@Table(name="userResource")
@Entity
class UserResource(
    @Column(unique = true, nullable = false)
    var username: String = "",
    @Column
    var password: ByteArray = ByteArray(0),
    @Column(unique = true, nullable = true)
    var email: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "owner")
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
    @Column(name = "id_user")
    var idUser: Long? = null,
) {
    fun addAccount(accountResource: AccountResource) {
        accountResource.owner = this
        accounts.add(accountResource)
    }
}