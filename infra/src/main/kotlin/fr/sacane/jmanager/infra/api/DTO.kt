package fr.sacane.jmanager.infra.api

import java.time.LocalDate
import java.time.Month

data class UserDTO(
    val id: Long,
    val username: String,
    val pseudonym: String,
    val email: String
)

data class RegisteredUserDTO(
    val id: Long,
    val username: String,
    val pseudonym: String,
    val email: String,
    val password: String
)

data class UserPasswordDTO(
    val username: String,
    val password: String,
    val id: Long
)

data class SheetDTO(
//    val id: Long,
    val label: String,
    val amount: Double,
    val action: Boolean,
    val date: LocalDate
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
    val userCredentials: UserCredentialsDTO,
    val labelAccount: String,
    val amount: Double
)

data class UserSheetDTO(
    val userCredentials: UserCredentialsDTO,
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
    val year: Int
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
    val pseudonym: String,
    val password: String,
)

data class UserTokenDTO(
    val user: UserDTO,
    val tokenPair: TokenDTO
)

data class UserIdTokenDTO(
    val userId: Long,
    val tokenPair: TokenDTO
)