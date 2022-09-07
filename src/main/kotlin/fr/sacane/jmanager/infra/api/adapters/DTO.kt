package fr.sacane.jmanager.infra.api.adapters

data class UserDTO(
    val id: Long,
    val username: String,
    val password: String,
    val pseudonym: String,
    val email: String
)
