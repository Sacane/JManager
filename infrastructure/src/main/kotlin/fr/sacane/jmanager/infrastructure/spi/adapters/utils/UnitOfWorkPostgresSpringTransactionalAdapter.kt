package fr.sacane.jmanager.infrastructure.spi.adapters.utils

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
@Adapter(Side.INFRASTRUCTURE)
class UnitOfWorkPostgresSpringTransactionalAdapter: UnitOfWorkTransactionProvider {

    @Transactional
    override fun <T, R> executeInTransaction(input: T, executable: (T) -> R): R {
        return executable(input)
    }
}