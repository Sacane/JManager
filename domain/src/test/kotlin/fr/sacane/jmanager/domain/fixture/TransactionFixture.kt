package fr.sacane.jmanager.domain.fixture

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object TransactionFixture {

    fun aTransaction(
        id: UUID? = UUID.randomUUID(),
        label: String = "Default transaction",
        amount: Amount = 100.toAmount(),
        isIncome: Boolean = true,
        date: LocalDate = LocalDate.of(2024, 1, 1),
        lastModified: LocalDateTime = LocalDateTime.now(),
        tag: Tag? = TagFixture.aDefaultTag(),
        isPreview: Boolean = false,
        regularTransactionId: RegularTransactionId? = null
    ) = Transaction(id, label, date, amount, isIncome, lastModified, isPreview, tag, regularTransactionId)
}
