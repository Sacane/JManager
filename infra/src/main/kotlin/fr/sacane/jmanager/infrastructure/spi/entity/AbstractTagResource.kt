package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*
import java.util.UUID

@MappedSuperclass
sealed class AbstractTagResource(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var idTag: UUID? = null,
    var name: String = "",
    @Embedded
    var color: Color = Color(0, 0, 0),
    @OneToMany(cascade = [(CascadeType.ALL)])
    var linkedTransaction: MutableSet<TransactionResource> = mutableSetOf(),
)

@Entity
class DefaultTagResource(
    idTag: UUID? = null,
    name: String = "",
    color: Color = Color(0, 0, 0)
) : AbstractTagResource(idTag, name, color)

@Entity
class TagPersonalResource(
    idTag: UUID? = null,
    name: String = "",
    color: Color = Color(0, 0, 0),
    @ManyToOne
    var owner: UserResource? = null
): AbstractTagResource(idTag, name, color)