package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
class SubscriptionEntity(
    @Column(length = 30)
    val label: String,
    val beginDate: LocalDate,
    val amount: BigDecimal,
    val isIncome: Boolean,

    @ManyToMany(cascade = [(CascadeType.DETACH)], mappedBy = "subscriptions")
    val accounts: MutableList<AccountResource>,
    @ManyToOne(cascade = [(CascadeType.DETACH)])
    var owner: UserResource? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    fun linkAccount(account: AccountResource) {
        accounts.add(account)
        account.subscriptions.add(this)
    }
}