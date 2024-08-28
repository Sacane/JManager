package fr.sacane.jmanager.infrastructure.rest.account

import fr.sacane.jmanager.infrastructure.rest.transaction.SheetDTO


data class AccountDTO(
    val id: Long?,
    val amount: String,
    val labelAccount: String,
    val sheets: List<SheetDTO>?
)

data class AccountInfoDTO(
    val amount: String,
    val label: String
)

data class UserAccountDTO(
    val id: Long,
    val labelAccount: String,
    val amount: String
)