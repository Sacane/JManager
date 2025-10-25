package fr.sacane.jmanager.infrastructure.api.admin

import java.time.LocalDateTime

data class UserForAdminResult(
    val id: String,
    val username: String,
    val email: String? = null,
    val isEnabled: Boolean,
    val roles: List<String>,
    val createdAt: LocalDateTime
)