package fr.sacane.jmanager.server.entity

import fr.sacane.jmanager.domain.hexadoc.DatasourceEntity
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
@DatasourceEntity
class CategoryResource(
    @Id
    @GeneratedValue
    @Column(unique = true, name = "id_category", nullable = false)
    var idCategory: Long?,
    @Column(name="label")
    var label: String?
){
    constructor(
        label: String?
    ): this(null, label)
}