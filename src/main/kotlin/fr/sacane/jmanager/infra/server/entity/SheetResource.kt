package fr.sacane.jmanager.infra.server.entity

import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table


@Entity
@Table(name="sheet")
open class SheetResource {

    @Id
    @GeneratedValue
    @Column(unique = true, name = "id_sheet", nullable = false)
    open var idSheet: Long? = null

    @Column(unique = true, name = "label_sheet")
    open var label: String? = null

    @Column(name= "amount")
    open var amount: Double? = null

    @Column(name="date")
    open var date: LocalDate? = null

    @Column(name="isEntry")
    open var isEntry: Boolean? = null

}
