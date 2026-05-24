package fr.sacane.jmanager.infrastructure.spi.entity

import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.SubscriptionPlan
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Table(name="user_resource")
@Entity
class UserResource(
    @Column(unique = true, nullable = false, length = 100)
    var username: String = "",
    @Column(length = 255)
    var password: String = "",
    @Column(unique = true, nullable = true, length = 255)
    var email: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "owner")
    var booklets: MutableList<BookletResource> = mutableListOf(),
    @OneToMany(mappedBy = "owner")
    var tags: MutableList<TagPersonalResource> = mutableListOf(),
    @Column(name = "creation_date", nullable = false, updatable = false)
    val creationDate: LocalDateTime = LocalDateTime.now(),
    @Column(name = "is_enabled", nullable = false)
    val isEnabled: Boolean = true,
    @Column(name = "projection_window_days", nullable = false)
    var projectionWindowDays: Int = 15,
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 50)
    var subscriptionPlan: SubscriptionPlan = SubscriptionPlan.BETA_TESTER,
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
    fun addBooklet(bookletResource: BookletResource) {
        bookletResource.owner = this
        booklets.add(bookletResource)
    }
    fun addTag(tag: TagPersonalResource) {
        tag.owner = this
        tags.add(tag)
    }
}
