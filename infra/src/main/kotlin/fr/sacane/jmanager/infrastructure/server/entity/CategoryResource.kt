package fr.sacane.jmanager.infrastructure.server.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class CategoryResource(
    @Id
    @GeneratedValue
    @Column(unique = true, name = "id_category", nullable = false)
    var idCategory: Long?,
    @Column(name="label")
    var label: String?
){
    constructor(): this(null, null)
    constructor(
        label: String?
    ): this(null, label)
}