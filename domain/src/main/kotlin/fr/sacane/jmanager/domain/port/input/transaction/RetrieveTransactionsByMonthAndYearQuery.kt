package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.models.SessionToken
import java.time.Month

data class RetrieveTransactionsByMonthAndYearQuery(
    val token: SessionToken,
    val month: Month,
    val year: Int,
    val bookletLabel: String
)
