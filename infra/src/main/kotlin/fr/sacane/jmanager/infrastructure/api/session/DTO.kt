package fr.sacane.jmanager.infrastructure.api.session

import fr.sacane.jmanager.infrastructure.configuration.BigDecimalSerializer
import fr.sacane.jmanager.infrastructure.configuration.LocalDateTimeSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class UserDTO(
    val id: Long,
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
    val id: Long? = null,
    val username: String,
    val email: String? = null,
    val token: String,
)