package fr.sacane.jmanager.domain.models

import java.util.UUID

data class BookletMonthlyCycleSetting(
    val bookletId: UUID,
    val bookletLabel: String,
    val monthlyPeriodStartDay: Int,
    val monthlyPeriodEndDay: Int?,
)

data class BookletMonthlyCycleUpdate(
    val monthlyPeriodStartDay: Int,
    val monthlyPeriodEndDay: Int?,
)

data class UserSettings(
    val projectionWindowDays: Int,
    val bookletCycles: List<BookletMonthlyCycleSetting>,
)
