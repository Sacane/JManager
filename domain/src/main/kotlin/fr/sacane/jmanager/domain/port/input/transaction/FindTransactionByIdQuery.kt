package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.models.SessionToken
import java.util.UUID

data class FindTransactionByIdQuery(
    val token: SessionToken,
    val id: UUID
)
