package fr.sacane.jmanager.infrastructure.spi.entity

import fr.sacane.jmanager.domain.models.transaction.Regularity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
data class RegularTransactionResource(
    val startDate: LocalDate,
    val label: String,
    @Column(name="amount", scale = 2)
    val amount: BigDecimal,
    val isIncome: Boolean,
    @Enumerated(EnumType.STRING)
    val regularity: Regularity,
    val currency: String = "€",

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "tag_id", referencedColumnName = "idTag")
    var tag: DefaultTagResource? = null,

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "personal_tag_id", referencedColumnName = "idTag")
    var personalTag:TagPersonalResource? = null,

    @Id
    @GeneratedValue
    val regularTransactionId: UUID? = null
)