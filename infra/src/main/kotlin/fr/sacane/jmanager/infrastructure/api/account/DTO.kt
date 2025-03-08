package fr.sacane.jmanager.infrastructure.api.account

import fr.sacane.jmanager.infrastructure.api.transaction.TransactionResult
import java.math.BigDecimal


data class AccountDTO(
    val id: Long?,
    val amount: BigDecimal,
    val labelAccount: String,
    val previewAmount: String,
    val sheets: List<TransactionResult>?,
    val currency: String
)

data class AccountInfoDTO(
    val amount: String,
    val label: String,
    val id: String,
    val currency: String
)

data class UserBookletRequest(
    val id: Long,
    val labelAccount: String,
    val amount: Double,
    val currency: String
)