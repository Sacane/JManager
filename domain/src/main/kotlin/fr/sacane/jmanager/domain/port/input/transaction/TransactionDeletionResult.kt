package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.models.Amount
import java.util.UUID

data class TransactionDeletionResult(
    val deletedIds: List<UUID>,
    val bookletAmount: Amount,
)
