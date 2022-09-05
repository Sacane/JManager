package fr.sacane.jmanager.infra.server.entity

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

    @Column(unique = true, name= "amount")
    open var amount: Double? = null

}
