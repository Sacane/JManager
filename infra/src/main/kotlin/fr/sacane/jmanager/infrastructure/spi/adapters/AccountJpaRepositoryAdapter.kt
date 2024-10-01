package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.AccountRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.AccountJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Repository

@Repository
class AccountJpaRepositoryAdapter(
    private val accountRepository: AccountJpaRepository,
    private val userRepository: UserPostgresRepository,
    private val accountMapper: AccountMapper
): AccountRepository {
    @Transactional
    override fun editFromAnother(account: Account): Account? {
        val accountFromDatabase = accountRepository.findByIdWithSheets(account.id!!) ?: return null
        accountFromDatabase.amount = account.sold.amount
        return accountFromDatabase.toModel()
    }

    @Transactional
    override fun getLastSheetPosition(accountId: Long): Int? {
        return accountRepository.findLastSheetPosition(accountId)
    }

    @Transactional
    override fun save(ownerId: UserId, account: Account): Account? {
        val id = ownerId.id ?: return null
        val user = userRepository.findByIdWithAccount(id) ?: return null
        val accountResource = accountMapper.asResource(account)
        user.addAccount(accountResource)
        return accountResource.toModel()
    }

    @Transactional
    override fun findAccountByIdWithTransactions(accountId: Long): Account? {
        val accountResponse = accountRepository.findByIdWithSheets(accountId)
        return accountResponse?.toModel()
    }

    override fun findAccountByLabelWithTransactions(userId: UserId, accountLabel: String): Account? {
        if(userId.id == null) return null
        return accountRepository.findByOwnerAndLabelWithSheets(userId.id!!, accountLabel)?.toModel() ?: return null
    }

    override fun deleteAccountById(accountId: Long) {
        accountRepository.deleteById(accountId)
    }

    override fun upsert(account: Account): Account {
        return accountRepository.save(accountMapper.asResource(account)).toModel()
    }
}