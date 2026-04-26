package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.Transaction

data class BookTransactionCommand(
    val token: SessionToken,
    val bookletLabel: String,
    val transaction: Transaction
)
