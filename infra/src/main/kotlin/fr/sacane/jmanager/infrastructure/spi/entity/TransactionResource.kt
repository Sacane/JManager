package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID


@Entity
@Table(name="sheet")
class TransactionResource(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true, name = "id_sheet", nullable = false)
    var idSheet: UUID? = null,
    @Column(name = "label_sheet")
    var label: String,
    @Column(name="date")
    var date: LocalDate = LocalDate.now(),
    @Column(name="expenses", scale = 2)
    var value: BigDecimal = BigDecimal(0.0),
    var isIncome: Boolean? = false,
    @Column(name="booklet_amount", scale = 2)
    var bookletAmount: BigDecimal = BigDecimal(0.0),
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    var tag: DefaultTagResource? = null,
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    var personalTag:TagPersonalResource? = null,
    var currency: String = "€",
    @Column(name="last_modified")
    var lastModified: LocalDateTime? = null,
    var isPreview: Boolean = false,
    @Column(name="regular_transaction_id")
    var regularTransactionId: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    var booklet: BookletResource? = null,
    ){
    override fun toString(): String {
        return """
            id: $idSheet
            label : $label
            date: $date
            value: $value
            bookletAmount: $bookletAmount
        """.trimIndent()
    }
}
