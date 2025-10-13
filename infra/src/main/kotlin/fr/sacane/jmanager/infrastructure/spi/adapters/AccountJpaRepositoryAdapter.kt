package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.AccountRepositoryPort
import fr.sacane.jmanager.infrastructure.spi.repositories.AccountJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class AccountJpaRepositoryAdapter(
    private val accountRepository: AccountJpaRepository,
    private val userRepository: UserPostgresRepository,
    private val accountMapper: AccountMapper
): AccountRepositoryPort {
    @Transactional
    override fun editFromAnother(booklet: Booklet): Booklet? {
        val accountFromDatabase = accountRepository.findByIdWithSheets(booklet.id!!) ?: return null
        accountFromDatabase.amount = booklet.amount.amount
        return accountFromDatabase.toModel()
    }

    @Transactional
    override fun save(ownerId: UserId, booklet: Booklet): Booklet? {
        val id = ownerId.value ?: return null
        val user = userRepository.findByIdWithAccount(id) ?: return null
        val accountResource = accountMapper.asResource(booklet)
        val accountSaved = accountRepository.save(accountResource)
        user.addAccount(accountSaved)
        return accountSaved.toModel()
    }

    override fun findAccountByIdWithTransactions(accountId: Long): Booklet? {
        val accountResponse = accountRepository.findByIdWithSheets(accountId)
        return accountResponse?.toModel()
    }

    override fun findAccountByLabelWithTransactions(userId: UserId, accountLabel: String): Booklet? {
        if(userId.value == null) return null
        return accountRepository.findByOwnerAndLabelWithSheets(userId.value!!, accountLabel)?.toModel() ?: return null
    }

    override fun deleteAccountById(accountId: Long) {
        val account = accountRepository.findByIdWithMonthlyTransactions(accountId) ?: return
        account.clearAllMonthlyTransactions()
        accountRepository.deleteById(accountId)
    }

    override fun upsert(booklet: Booklet): Booklet {
        return accountRepository.save(accountMapper.asResource(booklet)).also {
            for(transaction in it.sheets) {
                transaction.account = it
            }
        }.toModel()
    }
    override fun update(booklet: Booklet) {
        accountRepository.update(booklet.label, booklet.amount.amount, booklet.previewAmount.amount, booklet.id!!)
    }
}