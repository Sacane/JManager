package fr.sacane.jmanager.domain.models

import java.time.LocalDate

data class Sheet(
        val id: Long,
        val label: String,
        val date: LocalDate,
        val value: Double,
        val isEntry: Boolean,
        val category: Category = CategoryFactory.DEFAULT_CATEGORY
)
