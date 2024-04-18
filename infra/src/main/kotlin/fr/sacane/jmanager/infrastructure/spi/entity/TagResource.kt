package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*

@Entity
class TagResource(
    @Id
    @GeneratedValue
    var idTag: Long? = null,
    var name: String = "",
    @Embedded
    var color: Color = Color(0, 0, 0),
    var isDefault: Boolean = false,
    @ManyToOne
    var owner: UserResource? = null,
    @OneToMany(cascade = [(CascadeType.ALL)], mappedBy = "tag")
    var linkedTransaction: MutableSet<SheetResource> = mutableSetOf()
){
    override fun toString(): String {
        return "id = $idTag, name = $name, color = $color, isDefault = $isDefault"
    }
}
