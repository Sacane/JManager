package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*

@MappedSuperclass
sealed class AbstractTagResource(
    @Id
    @GeneratedValue
    var idTag: Long? = null,
    var name: String = "",
    @Embedded
    var color: Color = Color(0, 0, 0),
    @OneToMany(cascade = [(CascadeType.ALL)])
    var linkedTransaction: MutableSet<SheetResource> = mutableSetOf()
){
    fun addTransaction(transaction: SheetResource) {
        linkedTransaction.add(transaction)
        when(this) {
            is DefaultTagResource -> transaction.linkDefaultTag(this)
            is TagPersonalResource -> transaction.linkPersonalTag(this)
        }
    }
}

@Entity
class DefaultTagResource(
   idTag: Long? = null,
    name: String = "",
    color: Color = Color(0, 0, 0)
) : AbstractTagResource(idTag, name, color)

@Entity
class TagPersonalResource(
    idTag: Long? = null,
    name: String = "",
    color: Color = Color(0, 0, 0),
    @ManyToOne
    var owner: UserResource? = null
): AbstractTagResource(idTag, name, color)