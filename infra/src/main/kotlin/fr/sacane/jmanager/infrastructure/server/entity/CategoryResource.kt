package fr.sacane.jmanager.infrastructure.server.entity

import fr.sacane.jmanager.domain.hexadoc.DatasourceEntity
import jakarta.persistence.*

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