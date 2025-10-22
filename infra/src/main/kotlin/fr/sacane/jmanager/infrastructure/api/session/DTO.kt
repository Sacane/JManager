package fr.sacane.jmanager.infrastructure.api.session

import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    val id: String,
    val username: String,
    val email: String? = null
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