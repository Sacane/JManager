package fr.sacane.jmanager.infrastructure.api

import java.time.LocalDate
import java.time.Month

data class UserDTO(
    val id: Long,
    val username: String,
    val email: String
)

data class RegisteredUserDTO(
    val username: String,
    val email: String,
    val password: String
)

data class UserPasswordDTO(
    val username: String,
    val password: String,
    val id: Long
)

data class SheetDTO(
    val id: Long,
    val label: String,
    val amount: Double,
    val action: Boolean,
    val date: LocalDate
)
data class SheetsAndAverageDTO(
    val sheets: List<SheetDTO>,
    val sum: Double
)

data class SheetSendDTO(
    val label: String,
    val amount: Double,
    val action: String,
    val date: LocalDate
)

data class AccountInfoDTO(
    val amount: Double,
    val label: String
)

data class AccountDTO(
    val id: Long,
    val amount: Double,
    val labelAccount: String,
    val sheets: List<SheetDTO>?
)

data class UserAccountDTO(
    val id: Long,
    val labelAccount: String,
    val amount: Double
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

data class UserAccountSheetDateFilteredDTO(
    val userId: Long,
    val accountLabel: String,
    val month: Month,
    val year: Int,
//    val average: Long
)
data class UserCategoryDTO(
    val userId: Long,
    val label: String
)

data class TokenDTO(
    val token: String,
    val refreshToken: String
)

data class UserLoginDTO(val userId: Long, val password: String, val token: String, val refreshToken: String)
data class UserCredentialsDTO(
    val id: Long,
    val username: String,
    val password: String,
)

data class UserTokenDTO(
    val user: UserDTO,
    val tokenPair: TokenDTO
)