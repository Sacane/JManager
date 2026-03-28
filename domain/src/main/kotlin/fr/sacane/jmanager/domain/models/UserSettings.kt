package fr.sacane.jmanager.domain.models

import java.util.UUID

data class AccountMonthlyCycleSetting(
    val accountId: UUID,
    val accountLabel: String,
    val monthlyPeriodStartDay: Int,
)

data class UserSettings(
    val projectionWindowDays: Int,
    val accountCycles: List<AccountMonthlyCycleSetting>,
)
