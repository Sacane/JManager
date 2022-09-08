package fr.sacane.jmanager.infra.api.adapters

import fr.sacane.jmanager.domain.model.Sheet
import java.time.LocalDate

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
    val account: String,
    val date: LocalDate
)

data class AccountDTO(
    val amount: Double,
    val labelAccount: String,
    val sheets: List<SheetDTO>
)

data class UserSheetDTO(
    val userDTO: UserDTO

)