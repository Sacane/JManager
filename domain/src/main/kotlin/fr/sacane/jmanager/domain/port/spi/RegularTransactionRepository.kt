package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.models.RegularTransaction

interface RegularTransactionRepository {
    fun saveRegularTransaction(regularTransaction: RegularTransaction): RegularTransaction
}