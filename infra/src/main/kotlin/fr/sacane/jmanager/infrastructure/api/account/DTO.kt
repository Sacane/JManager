package fr.sacane.jmanager.infrastructure.api.account

import fr.sacane.jmanager.infrastructure.api.transaction.TransactionResult
import fr.sacane.jmanager.infrastructure.configuration.BigDecimalSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class AccountDTO(
    val id: Long?,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val labelAccount: String,
    val previewAmount: String,
    val sheets: List<TransactionResult>?,
    val currency: String
)

@Serializable
data class AccountInfoDTO(
    val amount: String,
    val label: String,
    val id: String,
    val currency: String
)

@Serializable
data class UserBookletRequest(
    val id: Long,
    val labelAccount: String,
    val amount: Double,
    val currency: String
)