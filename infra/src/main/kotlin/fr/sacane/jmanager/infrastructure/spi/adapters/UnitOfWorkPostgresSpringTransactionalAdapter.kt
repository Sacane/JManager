package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.spi.UnitOfWorkTransactionProviderPort
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
@Adapter(Side.INFRASTRUCTURE)
class UnitOfWorkPostgresSpringTransactionalAdapter: UnitOfWorkTransactionProviderPort {

    @Transactional
    override fun <T, R> executeInTransaction(input: T, executable: (T) -> R): R {
        return executable(input)
    }
}