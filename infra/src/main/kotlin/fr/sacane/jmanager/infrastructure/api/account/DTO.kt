package fr.sacane.jmanager.infrastructure.api.account

import fr.sacane.jmanager.infrastructure.api.transaction.SheetDTO


data class AccountDTO(
    val id: Long?,
    val amount: String,
    val labelAccount: String,
    val previewAmount: String,
    val sheets: List<SheetDTO>?
)

data class AccountInfoDTO(
    val amount: String,
    val label: String,
    val id: String,
    val currency: String
)

data class UserAccountDTO(
    val id: Long,
    val labelAccount: String,
    val amount: Double,
    val currency: String
)