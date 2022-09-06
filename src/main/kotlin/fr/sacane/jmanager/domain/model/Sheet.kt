package fr.sacane.jmanager.domain.model

import java.time.LocalDate

data class Sheet(
        val label: String,
        val date: LocalDate,
        val value: Double,
        val isEntry: Boolean
)
