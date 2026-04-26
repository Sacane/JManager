package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import java.util.UUID

data class UpdateRegularTransactionCommand(
    val token: SessionToken,
    val regularTransaction: RegularTransaction,
    val bookletIds: List<UUID>
)
