package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface LoadTransactionsForBookletForAMonthUseCase {
    fun handle(query: LoadTransactionsForBookletForAMonthQuery): Result<BookletLoadingResult>
}
