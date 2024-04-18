package fr.sacane.jmanager.infrastructure.spi.entity

import jakarta.persistence.Embeddable

@Embeddable
class Color(
    var red: Int = 0,
    var green: Int = 0,
    var blue: Int = 0
)