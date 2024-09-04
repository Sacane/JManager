package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.port.spi.AccountRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.AccountJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Repository

@Repository
class AccountJpaRepositoryAdapter(
    private val accountRepository: AccountJpaRepository
): AccountRepository {
    @Transactional
    override fun editFromAnother(account: Account): Account? {
        val accountFromDatabase = accountRepository.findByIdWithSheets(account.id!!) ?: return null
        accountFromDatabase.amount = account.sold.amount
        return accountFromDatabase.toModel()
    }
}