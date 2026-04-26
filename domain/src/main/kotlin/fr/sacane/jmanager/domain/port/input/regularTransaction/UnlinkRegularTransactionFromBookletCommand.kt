package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.models.SessionToken
import java.util.UUID

data class UnlinkRegularTransactionFromBookletCommand(
    val token: SessionToken,
    val transactionId: String,
    val bookletId: UUID
)
