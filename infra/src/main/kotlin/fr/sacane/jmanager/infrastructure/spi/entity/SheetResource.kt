package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate


@Entity
@Table(name="sheet")
class SheetResource(
    @Id
    @GeneratedValue
    @Column(unique = true, name = "id_sheet", nullable = false)
    var idSheet: Long? = null,
    @Column(name = "label_sheet")
    var label: String = "undefined",
    @Column(name="date")
    var date: LocalDate = LocalDate.now(),
    @Column(name="expenses", scale = 2)
    var value: BigDecimal = BigDecimal(0.0),
    var isIncome: Boolean? = false,
    @Column(name="account_amount", scale = 2)
    var accountAmount: BigDecimal = BigDecimal(0.0),
    @Column
    var position: Int = 0,
    @ManyToOne
    var tag: DefaultTagResource? = null,
    @ManyToOne
    var personalTag:TagPersonalResource? = null,
    var currency: String = "€"

){
    override fun toString(): String {
        return """
            label : $label
            date: $date
            value: $value
            accountAmount: $accountAmount
        """.trimIndent()
    }

    fun linkPersonalTag(tag: TagPersonalResource) {
        this.personalTag = tag
    }
    fun linkDefaultTag(tag: DefaultTagResource) {
        this.tag = tag
    }
}
