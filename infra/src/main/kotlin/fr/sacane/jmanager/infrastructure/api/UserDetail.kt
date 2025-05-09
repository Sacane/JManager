package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.User
import org.springframework.security.core.context.SecurityContextHolder

data class JmanagerUserAuthDetail(
    val id: Long,
    val username: String,
    val role: Role,
    val token: String
)

fun User.asAuthDetail(token: String, role: Role = Role.USER): JmanagerUserAuthDetail {
    return JmanagerUserAuthDetail(
        id = this.id.value!!,
        username = this.username,
        role = role,
        token = token
    )
}

val currentUser: JmanagerUserAuthDetail
    get() = try {
        SecurityContextHolder.getContext().authentication.principal as JmanagerUserAuthDetail
    } catch (e: Exception) {
        throw UnauthorizedRequestException(
            401,
            "Impossible de récupérer l'utilisateur courant, veuillez vous reconnecter"
        )
    }