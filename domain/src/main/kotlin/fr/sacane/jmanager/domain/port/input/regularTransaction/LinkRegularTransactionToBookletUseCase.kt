package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.utils.Result
import java.util.UUID

@Port(Side.APPLICATION)
interface LinkRegularTransactionToBookletUseCase {
    fun linkRegularTransactionToBooklet(token: SessionToken, transactionId: String, bookletId: UUID): Result<RegularTransaction>
}
