package fr.sacane.jmanager.domain.models

import java.math.BigDecimal
import java.util.UUID

data class BookletMonthlyCycleSetting(
    val bookletId: UUID,
    val bookletLabel: String,
    val monthlyPeriodStartDay: Int,
    val monthlyPeriodEndDay: Int?,
    /** The monthly budget the user aims at for this booklet; null when none is set. */
    val budgetTarget: BigDecimal? = null,
)

data class BookletMonthlyCycleUpdate(
    val monthlyPeriodStartDay: Int,
    val monthlyPeriodEndDay: Int?,
)

data class UserSettings(
    val projectionWindowDays: Int,
    val bookletCycles: List<BookletMonthlyCycleSetting>,
)
