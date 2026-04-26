package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.models.SessionToken
import java.time.LocalDate
import java.time.Month
import java.util.UUID

data class LoadTransactionsForBookletForAMonthQuery(
    val token: SessionToken,
    val bookletId: UUID,
    val month: Month,
    val year: Int,
    val startingMonth: Month? = null,
    val startingYear: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val pageNumber: Int = 0,
    val pageSize: Int = 10,
)
