package fr.sacane.jmanager.domain.model

import java.time.LocalDate

class Sheet(
        val label: String,
        val date: LocalDate,
        val value: Double,
        val labelAccount: String,
        val isEntry: Boolean
)
