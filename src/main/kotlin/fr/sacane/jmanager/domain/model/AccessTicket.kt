package fr.sacane.jmanager.domain.model

data class AccessTicket(
    val user: User,
    val isOK: Boolean,
    val role: Role
)

enum class Role {
    ADMIN, USER
}