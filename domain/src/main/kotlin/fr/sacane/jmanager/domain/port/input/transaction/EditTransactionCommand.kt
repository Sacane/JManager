package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.util.UUID

data class EditTransactionCommand(
    val token: SessionToken,
    val bookletID: UUID,
    val transaction: Transaction
)
