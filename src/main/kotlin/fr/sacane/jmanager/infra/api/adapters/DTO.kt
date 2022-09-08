package fr.sacane.jmanager.infra.api.adapters

import java.time.LocalDate
import java.time.Month

data class UserDTO(
    val id: Long,
    val username: String,
    val password: String,
    val pseudonym: String,
    val email: String
)

data class UserPasswordDTO(
    val username: String,
    val password: String
)

data class SheetDTO(
    val label: String,
    val amount: Double,
    val action: String,
    val date: LocalDate
)

data class AccountDTO(
    val amount: Double,
    val labelAccount: String,
    val sheets: List<SheetDTO>
)

data class UserAccount(
    val userId: Long,
    val labelAccount: String
)

data class UserSheetDTO(
    val userId: Long,
    val month: Month,
    val year: Int,
    val accountLabel: String
)