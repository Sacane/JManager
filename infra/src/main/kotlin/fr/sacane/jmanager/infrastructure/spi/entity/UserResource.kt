package fr.sacane.jmanager.infrastructure.spi.entity

import fr.sacane.jmanager.infrastructure.spi.entity.transaction.MonthlyRegularTransactionEntity
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
    var accounts: MutableList<AccountResource> = mutableListOf(),
    @OneToMany(mappedBy = "owner")
    var tags: MutableList<TagPersonalResource> = mutableListOf(),
    @OneToMany(mappedBy = "owner")
    var monthlyTransactions: MutableList<MonthlyRegularTransactionEntity> = mutableListOf(),
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
}
