package fr.sacane.jmanager.infrastructure.api.session

import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    val id: String,
    val username: String,
    val email: String? = null,
    val createdDate: String? = null,
    val roles: List<String> = emptyList()
)

@Serializable
data class RegisteredUserDTO(
    val username: String,
    val password: String,
    val confirmPassword: String
)

@Serializable
data class UserPasswordDTO(
    val username: String,
    val password: String
)

@Serializable
data class UserStorageDTO(
    val id: String? = null,
    val username: String,
    val email: String? = null,
    val token: String,
)

@Serializable
data class UserSettingsDTO(
    val projectionWindowDays: Int,
    val accountCycles: List<AccountMonthlyCycleDTO>,
)

@Serializable
data class AccountMonthlyCycleDTO(
    val accountId: String,
    val label: String,
    val monthlyPeriodStartDay: Int,
)

@Serializable
data class UserSettingsUpdateDTO(
    val projectionWindowDays: Int,
    val accountCycles: List<AccountMonthlyCycleUpdateDTO>,
)

@Serializable
data class AccountMonthlyCycleUpdateDTO(
    val accountId: String,
    val monthlyPeriodStartDay: Int,
)