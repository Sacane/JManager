package fr.sacane.jmanager.infrastructure.api.setup

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.infrastructure.State
import fr.sacane.jmanager.infrastructure.spi.adapters.AccountMapper
import fr.sacane.jmanager.infrastructure.spi.adapters.toModel
import fr.sacane.jmanager.infrastructure.spi.repositories.AccountJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class AccountStateTestAdapter(
    private val accountJpaRepository: AccountJpaRepository,
    private val accountMapper: AccountMapper
): State<Booklet, Booklet> {
    @Transactional
    override fun get(): Collection<Booklet> {
        return accountJpaRepository.findAll().map { it.toModel() }
    }

    override fun clear() {
        accountJpaRepository.deleteAll()
    }

    override fun init(initialState: Collection<Booklet>) {
        accountJpaRepository.saveAll(initialState.map { accountMapper.asResource(it) })
    }
}