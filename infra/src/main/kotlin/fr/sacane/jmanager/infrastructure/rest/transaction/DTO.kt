package fr.sacane.jmanager.infrastructure.rest.transaction

import com.fasterxml.jackson.annotation.JsonFormat
import fr.sacane.jmanager.infrastructure.rest.tag.TagDTO
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
    val accountAmount: String,
    val position: Int,
    val tagDTO: TagDTO? = null
)
data class SheetsAndAverageDTO(
    val sheets: List<SheetDTO>,
    val sum: Double
)

data class SheetSendDTO(
    val label: String,
    val date: LocalDate,
    val value: String,
    val isIncome: Boolean,
    val accountAmount: String
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