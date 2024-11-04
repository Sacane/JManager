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
    @OneToMany(cascade = [(CascadeType.DETACH)])
    val accounts: MutableList<AccountResource>,
    @ManyToOne(cascade = [(CascadeType.DETACH)])
    var owner: UserResource? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)