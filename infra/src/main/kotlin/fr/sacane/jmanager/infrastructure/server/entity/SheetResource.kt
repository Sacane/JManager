package fr.sacane.jmanager.infrastructure.server.entity

import fr.sacane.jmanager.domain.hexadoc.DatasourceEntity
import java.time.LocalDate
import jakarta.persistence.*


@Entity
@Table(name="sheet")
@DatasourceEntity
class SheetResource(
    @Id
    @GeneratedValue
    @Column(unique = true, name = "id_sheet", nullable = false)
    var idSheet: Long? = null,
    @Column(name = "label_sheet")
    var label: String? = null,
    @Column(name="date")
    var date: LocalDate? = null,
    @Column(name="expenses")
    var expenses: Double? = null,
    @Column(name="income")
    var income: Double? = null,
    @Column(name="account_amount")
    var accountAmount: Double? = null,
    @OneToOne
    @JoinTable(
        name = "sheet_category",
        joinColumns = [JoinColumn(name = "id_sheet")],
        inverseJoinColumns = [JoinColumn(name = "id_category")]
    )
    var category: CategoryResource?=null,
    @Column
    var position: Int? = 0

){
    constructor(
        label: String?,
        date: LocalDate?,
        expenses: Double?,
        income: Double?,
        category: CategoryResource?,
        accountAmount: Double,
        position: Int
    ): this(null, label, date, expenses, income, accountAmount, category, position)

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
