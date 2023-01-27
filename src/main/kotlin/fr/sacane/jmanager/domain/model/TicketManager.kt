package fr.sacane.jmanager.domain.model

import java.time.LocalDateTime
import java.util.*

data class Token(val id: UUID, val lastRefresh: LocalDateTime, val refreshToken: UUID)

data class Ticket(
    val user: User,
    val hasAccess: Boolean,
    val token: Token?
)
