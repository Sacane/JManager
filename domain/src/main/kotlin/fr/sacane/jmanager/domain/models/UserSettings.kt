package fr.sacane.jmanager.domain.models

import java.util.UUID

data class AccountMonthlyCycleSetting(
    val accountId: UUID,
    val accountLabel: String,
    val monthlyPeriodStartDay: Int,
    val monthlyPeriodEndDay: Int?,
)

data class AccountMonthlyCycleUpdate(
    val monthlyPeriodStartDay: Int,
    val monthlyPeriodEndDay: Int?,
)

data class UserSettings(
    val projectionWindowDays: Int,
    val accountCycles: List<AccountMonthlyCycleSetting>,
)
