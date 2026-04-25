package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.BookletBalances
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.utils.Result
import java.time.LocalDate
import java.time.Month
import java.util.UUID

@Port(Side.APPLICATION)
interface LoadBalancesForBookletForAMonthUseCase {
    fun loadBalancesForBookletForAMonth(
        token: SessionToken,
        bookletId: UUID,
        month: Month,
        year: Int,
        startingMonth: Month?,
        startingYear: Int?,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): Result<BookletBalances>
}
