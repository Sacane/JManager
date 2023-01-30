package fr.sacane.jmanager.domain.model

import java.time.LocalDateTime
import java.util.*
data class Token(
    val id: UUID,
    val lastRefresh: LocalDateTime,
    val refreshToken: UUID
)

enum class TicketState{
    AUTHENTICATED,
    TIMEOUT,
    INVALID
}

data class Ticket(
    val user: User?,
    val state: TicketState,
    val token: Token?
)

fun invalidateTicket(): Ticket{
    return Ticket(null, TicketState.INVALID, null)
}
fun expiredTicket(): Ticket{
    return Ticket(null, TicketState.TIMEOUT, null)
}

fun emptyValidateTicket(): Ticket{
    return Ticket(null ,TicketState.AUTHENTICATED, null)
}