package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.models.SessionToken

data class DeleteRegularTransactionCommand(
    val token: SessionToken,
    val transactionId: String
)
