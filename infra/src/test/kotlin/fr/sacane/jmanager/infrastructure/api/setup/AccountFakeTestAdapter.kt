package fr.sacane.jmanager.infrastructure.api.setup

import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.infrastructure.State
import fr.sacane.jmanager.infrastructure.spi.adapters.asResource
import fr.sacane.jmanager.infrastructure.spi.adapters.toModel
import fr.sacane.jmanager.infrastructure.spi.repositories.AccountJpaRepository
import org.springframework.stereotype.Component

@Component
class AccountFakeTestAdapter(
    private val accountJpaRepository: AccountJpaRepository
): State<Account> {
    override fun get(): Collection<Account> {
        return accountJpaRepository.findAll().map { it.toModel() }
    }

    override fun clear() {
        accountJpaRepository.deleteAll()
    }

    override fun init(initialState: Collection<Account>) {
        accountJpaRepository.saveAll(initialState.map { it.asResource() })
    }
}