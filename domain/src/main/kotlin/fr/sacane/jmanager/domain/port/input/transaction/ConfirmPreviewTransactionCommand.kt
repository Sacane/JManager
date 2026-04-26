package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.SessionToken
import java.time.LocalDate
import java.util.UUID

data class ConfirmPreviewTransactionCommand(
    val token: SessionToken,
    val bookletID: UUID,
    val transactionId: UUID,
    val newAmount: Amount? = null,
    val newDate: LocalDate? = null
)
