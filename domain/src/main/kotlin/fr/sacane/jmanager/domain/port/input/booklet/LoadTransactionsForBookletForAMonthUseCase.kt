package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.api.BookletLoadingResult
import fr.sacane.jmanager.domain.utils.Result
import java.time.LocalDate
import java.time.Month
import java.util.UUID

@Port(Side.APPLICATION)
interface LoadTransactionsForBookletForAMonthUseCase {
    fun loadTransactionsForBookletForAMonth(
        token: SessionToken,
        bookletId: UUID,
        month: Month,
        year: Int,
        startingMonth: Month?,
        startingYear: Int?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageNumber: Int,
        pageSize: Int,
    ): Result<BookletLoadingResult>
}
