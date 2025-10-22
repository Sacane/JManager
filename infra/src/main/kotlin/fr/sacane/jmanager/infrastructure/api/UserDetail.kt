package fr.sacane.jmanager.infrastructure.api

import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.User
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

data class JmanagerUserAuthDetail(
    val id: UUID,
    val username: String,
    val role: Set<Role>,
    val token: String
)

fun User.asAuthDetail(token: String, role: Set<Role> = setOf(Role.USER)): JmanagerUserAuthDetail {
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