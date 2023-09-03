package fr.sacane.jmanager.domain.models

import java.time.LocalDate

class Sheet(
    val id: Long?,
    val label: String,
    val date: LocalDate,
    val expenses: Double,
    val income: Double,
    var accountAmount: Double,
    val category: Category = CategoryFactory.DEFAULT_CATEGORY,
    var position: Int = 0
)