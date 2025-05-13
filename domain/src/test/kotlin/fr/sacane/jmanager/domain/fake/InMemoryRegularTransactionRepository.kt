package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.State
import fr.sacane.jmanager.domain.models.RegularTransaction
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository

class InMemoryRegularTransactionRepository: RegularTransactionRepository, State<RegularTransaction> {

    private val transactions = mutableListOf<RegularTransaction>()

    override fun saveRegularTransaction(regularTransaction: RegularTransaction): RegularTransaction {
        transactions.add(regularTransaction)
        return regularTransaction
    }

    override fun getStates(): Collection<RegularTransaction> {
        return transactions
    }

    override fun clear() {
        transactions.clear()
    }

    override fun init(initialState: Collection<RegularTransaction>) {
        transactions.addAll(initialState)
    }
}