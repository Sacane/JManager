package fr.sacane.jmanager.infrastructure.server.entity

import fr.sacane.jmanager.domain.hexadoc.DatasourceEntity
import java.time.LocalDate
import javax.persistence.*


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
    @OneToOne
    @JoinTable(
        name = "sheet_category",
        joinColumns = [JoinColumn(name = "id_sheet")],
        inverseJoinColumns = [JoinColumn(name = "id_category")]
    )
    var category: CategoryResource?=null

){
    constructor(
        label: String?,
        date: LocalDate?,
        expenses: Double?,
        income: Double?,
        category: CategoryResource?
    ): this(null, label, date, expenses, income, category)
}
