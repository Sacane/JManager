package fr.sacane.jmanager.infrastructure.rest.session

data class UserDTO(
    val id: Long,
    val username: String,
    val email: String? = null
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

data class UserStorageDTO(
    val id: Long? = null,
    val username: String,
    val email: String? = null,
    val token: String,
    val refreshToken: String
)