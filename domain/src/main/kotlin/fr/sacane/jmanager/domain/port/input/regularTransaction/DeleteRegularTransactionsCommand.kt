package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.models.SessionToken

data class DeleteRegularTransactionsCommand(
    val token: SessionToken,
    val transactionIds: List<String>
)
