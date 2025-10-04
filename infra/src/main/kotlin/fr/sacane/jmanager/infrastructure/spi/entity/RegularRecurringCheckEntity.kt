package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
data class RegularRecurringCheckEntity(
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    val id: Long,
    val year: Int,
    val month: Int,
    val numberSavedTransaction: Int,
    @ManyToOne
    @JoinColumn(name = "id_user")
    val user: UserResource,
)
