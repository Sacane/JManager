package fr.sacane.jmanager.infrastructure.spi.entity

import fr.sacane.jmanager.domain.models.Role
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime
import java.util.UUID

@Table(name="user_resource")
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
    @Column(name = "creation_date", nullable = false, updatable = false)
    val creationDate: LocalDateTime = LocalDateTime.now(),
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_role",
        joinColumns = [JoinColumn(name = "id_user", referencedColumnName = "id_user")]
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    var roles: MutableSet<Role> = mutableSetOf(Role.USER),
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user")
    var idUser: UUID? = null,
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
