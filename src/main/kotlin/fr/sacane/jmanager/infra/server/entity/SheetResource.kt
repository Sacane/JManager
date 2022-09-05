package fr.sacane.jmanager.infra.server.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id


@Entity
open class SheetResource {

    @Id
    @GeneratedValue
    @Column(unique = true, name = "id_sheet", nullable = false)
    var idSheet: Int? = null

    @Column(unique = true, name = "label_sheet")
    var label: String? = null

    @Column(unique = true, name= "amount")
    var amount: Double? = null

}
