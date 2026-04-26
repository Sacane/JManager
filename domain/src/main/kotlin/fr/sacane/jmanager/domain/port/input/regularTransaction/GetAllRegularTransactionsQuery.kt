package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.models.SessionToken

data class GetAllRegularTransactionsQuery(
    val token: SessionToken,
    val pageNumber: Int = 0,
    val pageSize: Int = 10
)
