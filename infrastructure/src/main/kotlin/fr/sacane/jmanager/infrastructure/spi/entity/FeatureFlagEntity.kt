package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "feature_flag")
class FeatureFlagEntity(
    @Id
    @Column(name = "key", nullable = false, length = 100)
    val key: String = "",
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = false,
)
