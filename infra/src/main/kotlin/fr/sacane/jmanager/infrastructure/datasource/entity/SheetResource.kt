package fr.sacane.jmanager.infrastructure.datasource.entity

import jakarta.persistence.*
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
    @Column(name="expenses")
    var expenses: Double = 0.0,
    @Column(name="income")
    var income: Double = 0.0,
    @Column(name="account_amount")
    var accountAmount: Double = 0.0,
    @OneToOne
    @JoinTable(
        name = "sheet_category",
        joinColumns = [JoinColumn(name = "id_sheet")],
        inverseJoinColumns = [JoinColumn(name = "id_category")]
    )
    var category: CategoryResource? = null,
    @Column
    var position: Int = 0

){
    override fun toString(): String {
        return """
            label : $label
            date: $date
            expenses: $expenses
            income: $income
            accountAmount: $accountAmount
        """.trimIndent()
    }
}
