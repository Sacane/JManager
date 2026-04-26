package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.models.SessionToken
import java.time.Month
import java.util.UUID

data class RegenerateDeletedPrevisionalTransactionsCommand(
    val token: SessionToken,
    val bookletId: UUID,
    val month: Month,
    val year: Int
)
