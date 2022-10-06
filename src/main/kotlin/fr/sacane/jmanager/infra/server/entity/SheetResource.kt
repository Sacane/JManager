package fr.sacane.jmanager.infra.server.entity

import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.OneToOne
import javax.persistence.Table


@Entity
@Table(name="sheet")
class SheetResource(
    @Id
    @GeneratedValue
    @Column(unique = true, name = "id_sheet", nullable = false)
    var idSheet: Long? = null,
    @Column(unique = true, name = "label_sheet")
    var label: String? = null,
    @Column(name= "amount", unique = false)
    var amount: Double? = null,
    @Column(name="date")
    var date: LocalDate? = null,
    @Column(name="isEntry")
    var isEntry: Boolean? = null

){
    constructor(
        label: String?,
        amount: Double?,
        date: LocalDate?,
        isEntry: Boolean?,
        category: CategoryResource
    ): this(null, label, amount, date, isEntry)
}
