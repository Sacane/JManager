package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime


@Entity
@Table(name="sheet")
class TransactionResource(
    @Id
    @GeneratedValue
    @Column(unique = true, name = "id_sheet", nullable = false)
    var idSheet: Long? = null,
    @Column(name = "label_sheet")
    var label: String,
    @Column(name="date")
    var date: LocalDate = LocalDate.now(),
    @Column(name="expenses", scale = 2)
    var value: BigDecimal = BigDecimal(0.0),
    var isIncome: Boolean? = false,
    @Column(name="account_amount", scale = 2)
    var accountAmount: BigDecimal = BigDecimal(0.0),
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    var tag: DefaultTagResource? = null,
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    var personalTag:TagPersonalResource? = null,
    var currency: String = "€",
    @Column(name="last_modified")
    var lastModified: LocalDateTime? = null,
    var isPreview: Boolean = false,
    @ManyToOne(fetch = FetchType.LAZY)
    var account: BookletResource? = null,
    ){
    override fun toString(): String {
        return """
            id: $idSheet
            label : $label
            date: $date
            value: $value
            accountAmount: $accountAmount
        """.trimIndent()
    }
}
