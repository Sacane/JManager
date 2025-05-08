package fr.sacane.jmanager.infrastructure.api.transaction

import com.fasterxml.jackson.annotation.JsonFormat
import fr.sacane.jmanager.infrastructure.api.tag.TagDTO
import fr.sacane.jmanager.infrastructure.configuration.BigDecimalSerializer
import fr.sacane.jmanager.infrastructure.configuration.LocalDateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class UserAccountIdsTransactionRequest(
    val userId: Long,
    val accountId: Long,
    val sheet: TransactionResult
)


@Serializable
data class TransactionResult(
    val id: Long?,
    val label: String,
    @Serializable(with = BigDecimalSerializer::class)
    val value: BigDecimal,
    val currency: String = "€",
    val isIncome: Boolean,
    val date: String,
    val tagDTO: TagDTO? = null,
    val isPreview: Boolean
)
data class TransactionListResponse(
    val sheets: List<TransactionResult>
)

@Serializable
data class TransactionResponse(
    val id: String,
    val label: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val value: String,
    val isIncome: Boolean,
    val tagDTO: TagDTO,
    val accountAmount: String,
    val accountPreviewAmount: String,
    val isPreview: Boolean
)

@Serializable
data class UserBookletResponse(
    val accountLabel: String,
    val transactionResult: TransactionResult
)

@Serializable
data class AccountTransactionsIdRequest(
    val accountId: Long,
    val sheetIds: List<Long>
)