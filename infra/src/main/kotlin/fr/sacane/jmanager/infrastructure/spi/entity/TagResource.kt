package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
data class TagResource(
    @Id
    @GeneratedValue
    val idTag: Long? = null,
    val name: String
)