package fr.sacane.jmanager.infrastructure.spi.adapters.transaction

import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.output.repository.TransactionQueryRepository
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toModel
import fr.sacane.jmanager.infrastructure.spi.repositories.TransactionQueryJpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class TransactionQueryRepositoryJpaAdapter(
    private val jpaRepository: TransactionQueryJpaRepository
) : TransactionQueryRepository {

    override fun findByBookletIdAndDateBetween(
        bookletId: UUID,
        from: LocalDate,
        to: LocalDate
    ): List<Transaction> {
        return jpaRepository.findByBookletIdAndDateBetween(bookletId, from, to)
            .map { it.toModel() }
    }
}

