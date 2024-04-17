package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.Embeddable

@Embeddable
class Color(
    var red: Int,
    var green: Int,
    var blue: Int
)