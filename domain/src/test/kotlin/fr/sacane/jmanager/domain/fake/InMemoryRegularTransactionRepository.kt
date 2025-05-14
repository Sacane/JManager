package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.RegularTransactionId
import fr.sacane.jmanager.domain.models.transaction.Regularity
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository
import java.time.LocalDate
import java.util.*

class InMemoryRegularTransactionRepository: RegularTransactionRepository, State<RegularTransaction> {

    private val transactions = mutableListOf<RegularTransaction>()

    override fun getStates(): Collection<RegularTransaction> {
        return transactions
    }

    override fun clear() {
        transactions.clear()
    }

    override fun init(initialState: Collection<RegularTransaction>) {
        transactions.addAll(initialState)
    }

    override fun saveRegularTransaction(
        userId: UserId,
        startDate: LocalDate,
        label: String,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag,
        regularity: Regularity
    ): RegularTransaction {
        val transaction = RegularTransaction(
            id = RegularTransactionId("${userId.value}-${UUID.randomUUID()}"),
            startDate = startDate,
            label = label,
            amount = amount,
            isIncome = isIncome,
            tag = tag,
            regularity = regularity
        )
        transactions.add(transaction)
        return transaction
    }
}