package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
data class TagResource(
    @Id
    @GeneratedValue
    var idTag: Long? = null,
    var name: String = "",

    @ManyToOne
    var owner: UserResource? = null
)