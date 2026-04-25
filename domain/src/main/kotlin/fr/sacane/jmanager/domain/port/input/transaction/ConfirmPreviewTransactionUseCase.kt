package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.utils.Result
import java.time.LocalDate
import java.util.UUID

@Port(Side.APPLICATION)
interface ConfirmPreviewTransactionUseCase {
    fun confirmPreviewTransaction(
        token: SessionToken,
        bookletID: UUID,
        transactionId: UUID,
        newAmount: Amount?,
        newDate: LocalDate?
    ): Result<TransactionResumeResult>
}
