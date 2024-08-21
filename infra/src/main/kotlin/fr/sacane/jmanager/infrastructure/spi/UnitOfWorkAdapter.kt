package fr.sacane.jmanager.infrastructure.spi

import fr.sacane.jmanager.domain.port.spi.InfraTransactionProviderPort
import jakarta.transaction.Transactional

open class UnitOfWorkAdapter: InfraTransactionProviderPort {

    @Transactional
    override fun <T, R> executeInTransaction(input: T, executable: (T) -> R): R {
        return executable(input)
    }
}