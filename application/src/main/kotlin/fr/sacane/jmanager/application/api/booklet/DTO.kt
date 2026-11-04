package fr.sacane.jmanager.application.api.booklet

import fr.sacane.jmanager.application.api.transaction.TransactionResult
import fr.sacane.jmanager.application.configuration.BigDecimalSerializer
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class BookletDTO(
    val id: String?,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val label: String,
    val transactions: List<TransactionResult>?,
    val currency: String,
    /** Where the user placed this booklet, from 0; null until they order their booklets. */
    val displayPosition: Int? = null,
)

@Serializable
data class BookletInfoDTO(
    val amount: String,
    val label: String,
    val id: String,
    val currency: String
)

@Serializable
data class BookletBookingRequest(
    @field:NotBlank
    @field:Size(max = 30)
    val label: String,
    val amount: Double,
    @field:NotBlank
    val currency: String
)