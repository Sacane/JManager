package fr.sacane.jmanager.domain.model

import java.time.LocalDateTime
import java.util.*
data class Token(
    val id: UUID,
    val lastRefresh: LocalDateTime,
    val refreshToken: UUID
)

enum class TicketState{
    OK,
    TIMEOUT,
    INVALID;

    fun isSuccess(): Boolean{
        return this == OK
    }
    fun isFailure(): Boolean{
        return this != OK
    }
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
    return Ticket(null ,TicketState.OK, null)
}