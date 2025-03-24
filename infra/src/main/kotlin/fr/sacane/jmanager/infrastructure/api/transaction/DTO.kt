package fr.sacane.jmanager.infrastructure.api.transaction

import com.fasterxml.jackson.annotation.JsonFormat
import fr.sacane.jmanager.infrastructure.api.tag.TagDTO
import java.math.BigDecimal
import java.time.LocalDate

data class UserAccountIdsTransactionRequest(
    val userId: Long,
    val accountId: Long,
    val sheet: TransactionResult
)



data class TransactionResult(
    val id: Long?,
    val label: String,
    val value: BigDecimal,
    val currency: String = "€",
    val isIncome: Boolean,
    @JsonFormat(pattern = "dd-MM-yyyy")
    val date: LocalDate,
    val tagDTO: TagDTO? = null,
    val isPreview: Boolean
)
data class TransactionListResponse(
    val sheets: List<TransactionResult>
)

data class TransactionResponse(
    val id: String,
    val label: String,
    @JsonFormat(pattern = "dd-MM-yyyy")
    val date: LocalDate,
    val value: String,
    val isIncome: Boolean,
    val tagDTO: TagDTO,
    val accountAmount: String,
    val accountPreviewAmount: String,
    val isPreview: Boolean
)

data class UserBookletResponse(
    val userId: Long,
    val accountLabel: String,
    val transactionResult: TransactionResult
)

data class AccountTransactionsIdRequest(
    val accountId: Long,
    val sheetIds: List<Long>
)