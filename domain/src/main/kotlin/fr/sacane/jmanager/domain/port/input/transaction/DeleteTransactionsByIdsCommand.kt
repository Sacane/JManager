package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.models.SessionToken
import java.util.UUID

data class DeleteTransactionsByIdsCommand(
    val token: SessionToken,
    val bookletID: UUID,
    val transactionIds: List<UUID>
)
