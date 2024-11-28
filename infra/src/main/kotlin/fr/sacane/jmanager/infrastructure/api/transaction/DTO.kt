package fr.sacane.jmanager.infrastructure.api.transaction

import com.fasterxml.jackson.annotation.JsonFormat
import fr.sacane.jmanager.infrastructure.api.tag.TagDTO
import java.time.LocalDate
import java.time.Month

data class UserIDSheetDTO(
    val userId: Long,
    val accountId: Long,
    val sheet: SheetDTO
)



data class SheetDTO(
    val id: Long?,
    val label: String,
    val value: String,
    val currency: String = "€",
    val isIncome: Boolean,
    @JsonFormat(pattern = "dd-MM-yyyy")
    val date: LocalDate,
    val tagDTO: TagDTO? = null,
    val isPreview: Boolean
)
data class TransactionList(
    val sheets: List<SheetDTO>
)

data class TransactionResultDTO(
    val id: String,
    val label: String,
    val date: LocalDate,
    val value: String,
    val isIncome: Boolean,
    val tagDTO: TagDTO,
    val accountAmount: String,
    val accountPreviewAmount: String,
    val isPreview: Boolean
)

data class UserSheetDTO(
    val userId: Long,
    val month: Month,
    val year: Int,
    val accountLabel: String
)

data class UserAccountSheetDTO(
    val userId: Long,
    val accountLabel: String,
    val sheetDTO: SheetDTO
)

data class AccountSheetIdsDTO(
    val accountId: Long,
    val sheetIds: List<Long>
)