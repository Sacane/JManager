package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*

@Table(name="userResource")
@Entity
class UserResource(
    @Column(unique = true, nullable = false)
    var username: String = "",
    @Column
    var password: String = "",
    @Column(unique = true, nullable = true)
    var email: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "owner")
    var accounts: MutableList<BookletResource> = mutableListOf(),
    @OneToMany(mappedBy = "owner")
    var tags: MutableList<TagPersonalResource> = mutableListOf(),
    @Id
    @GeneratedValue
    @Column(name = "id_user")
    var idUser: Long? = null,
) {
    fun addAccount(bookletResource: BookletResource) {
        bookletResource.owner = this
        accounts.add(bookletResource)
    }
    fun addTag(tag: TagPersonalResource) {
        tag.owner = this
        tags.add(tag)
    }
}
