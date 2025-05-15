package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.Regularity
import java.time.LocalDate

interface RegularTransactionRepository {
    fun saveRegularTransaction(
        userId: UserId,
        startDate: LocalDate,
        label: String,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag,
        regularity: Regularity
    ): RegularTransaction

    fun getAllRegularTransactions(userId: UserId): List<RegularTransaction>
}