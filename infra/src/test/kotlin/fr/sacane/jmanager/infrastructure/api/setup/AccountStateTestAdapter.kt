package fr.sacane.jmanager.infrastructure.api.setup

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.infrastructure.State
import fr.sacane.jmanager.infrastructure.spi.adapters.AccountMapper
import fr.sacane.jmanager.infrastructure.spi.adapters.toModel
import fr.sacane.jmanager.infrastructure.spi.repositories.BookletJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class AccountStateTestAdapter(
    private val bookletJpaRepository: BookletJpaRepository,
    private val accountMapper: AccountMapper
): State<Booklet, Booklet> {
    @Transactional
    override fun get(): Collection<Booklet> {
        return bookletJpaRepository.findAll().map { it.toModel() }
    }

    override fun clear() {
        bookletJpaRepository.deleteAll()
    }

    override fun init(initialState: Collection<Booklet>) {
        bookletJpaRepository.saveAll(initialState.map { accountMapper.asResource(it) })
    }
}