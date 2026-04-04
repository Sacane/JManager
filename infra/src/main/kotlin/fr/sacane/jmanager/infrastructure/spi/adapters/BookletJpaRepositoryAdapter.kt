package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.BookletMapper
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toModel
import fr.sacane.jmanager.infrastructure.spi.repositories.BookletJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class BookletJpaRepositoryAdapter(
    private val accountRepository: BookletJpaRepository,
    private val userRepository: UserPostgresRepository,
    private val bookletMapper: BookletMapper
): BookletRepository {
    @Transactional
    override fun editFromAnother(booklet: Booklet): Booklet? {
        val id = booklet.id ?: return null
        val accountFromDatabase = accountRepository.findByIdWithSheets(id) ?: return null
        accountFromDatabase.amount = booklet.amount.value
        return accountFromDatabase.toModel()
    }

    @Transactional
    override fun save(ownerId: UserId, booklet: Booklet): Booklet? {
        val id = ownerId.value ?: return null
        val user = userRepository.findByIdWithAccount(id) ?: return null
        val accountResource = bookletMapper.asResource(booklet)
        val accountSaved = accountRepository.save(accountResource)
        user.addAccount(accountSaved)
        return accountSaved.toModel()
    }

    @Transactional
    override fun findBookletByIdWithTransactions(bookletId: UUID): Booklet? {
        val accountResponse = accountRepository.findByIdWithSheets(bookletId)
        return accountResponse?.toModel()
    }

    @Transactional
    override fun findBookletByLabelWithTransactions(userId: UserId, bookletLabel: String): Booklet? {
        val id = userId.value ?: return null
        return accountRepository.findByOwnerAndLabelWithSheets(id, bookletLabel)?.toModel()
    }

    @Transactional
    override fun deleteBookletById(bookletId: UUID) {
        val booklet = accountRepository.findByIdWithRegularTransactions(bookletId) ?: return
        booklet.clearAllRegularTransactions()
        accountRepository.deleteById(bookletId)
    }

    @Transactional
    override fun upsert(booklet: Booklet): Booklet {
        return accountRepository.save(bookletMapper.asResource(booklet)).also {
            for(transaction in it.sheets) {
                transaction.account = it
            }
        }.toModel()
    }
    override fun update(booklet: Booklet) {
        val id = booklet.id ?: return
        accountRepository.update(booklet.label, booklet.amount.value, id)
    }

    @Transactional
    override fun updateMonthlyPeriodStartDay(accountId: UUID, monthlyPeriodStartDay: Int, monthlyPeriodEndDay: Int?): Boolean {
        return accountRepository.updateMonthlyPeriodStartDay(accountId, monthlyPeriodStartDay, monthlyPeriodEndDay) > 0
    }

    @Transactional
    override fun findBookletsForUser(userId: UserId): List<Booklet> {
        return userId.value?.let { id -> accountRepository.findAllBookletsByUserId(id).map { it.toModel() } } ?: emptyList()
    }
}