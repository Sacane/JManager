package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*

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
    @OneToMany(mappedBy = "owner")
    var tags: MutableList<TagPersonalResource> = mutableListOf(),
    @Id
    @GeneratedValue
    @Column(name = "id_user")
    var idUser: Long? = null,
) {
    fun addAccount(accountResource: AccountResource) {
        accountResource.owner = this
        accounts.add(accountResource)
    }
    fun addTag(tag: TagPersonalResource) {
        tag.owner = this
        tags.add(tag)
    }
    fun addAllTag(tags: List<TagPersonalResource>) {
        tags.forEach {
            addTag(it)
        }
    }
}
