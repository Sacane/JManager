package fr.sacane.jmanager.infrastructure.api.booklet

import fr.sacane.jmanager.infrastructure.api.transaction.TransactionResult
import fr.sacane.jmanager.infrastructure.configuration.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class AccountDTO(
    val id: String?,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val labelAccount: String,
    val transactions: List<TransactionResult>?,
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
data class BookletBookingRequest(
    val labelAccount: String,
    val amount: Double,
    val currency: String
)