package fr.sacane.jmanager.infrastructure.rest.account

import fr.sacane.jmanager.infrastructure.rest.sheet.SheetDTO


data class AccountDTO(
    val id: Long?,
    val amount: Double,
    val labelAccount: String,
    val sheets: List<SheetDTO>?
)

data class AccountInfoDTO(
    val amount: Double,
    val label: String
)

data class UserAccountDTO(
    val id: Long,
    val labelAccount: String,
    val amount: Double
)