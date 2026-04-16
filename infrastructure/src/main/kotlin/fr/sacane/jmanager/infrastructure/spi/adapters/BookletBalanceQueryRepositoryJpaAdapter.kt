package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.port.spi.repository.BookletBalanceQueryRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.BookletBalanceJpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class BookletBalanceQueryRepositoryJpaAdapter(
    private val jpaRepository: BookletBalanceJpaRepository
) : BookletBalanceQueryRepository {

    override fun findPersistedBalances(bookletId: UUID): BookletBalanceQueryRepository.PersistedBalances? {
        val row = jpaRepository.findPersistedBalances(bookletId) ?: return null
        return BookletBalanceQueryRepository.PersistedBalances(
            label = row.label,
            amount = row.amount,
        )
    }
}
