package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.BookletMapper
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toModel
import fr.sacane.jmanager.infrastructure.spi.repositories.BookletJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class BookletJpaRepositoryAdapter(
    private val bookletRepository: BookletJpaRepository,
    private val userRepository: UserPostgresRepository,
    private val bookletMapper: BookletMapper
): BookletRepository {
    @Transactional
    override fun editFromAnother(booklet: Booklet): Booklet? {
        val id = booklet.id ?: return null
        val bookletFromDatabase = bookletRepository.findByIdWithTransactions(id) ?: return null
        bookletFromDatabase.amount = booklet.amount.value
        return bookletFromDatabase.toModel()
    }

    @CacheEvict(cacheNames = ["allBooklets"], key = "#ownerId")
    @Transactional
    override fun save(ownerId: UserId, booklet: Booklet): Booklet? {
        val id = ownerId.value ?: return null
        val user = userRepository.findByIdWithBooklets(id) ?: return null
        val bookletResource = bookletMapper.asResource(booklet)
        val bookletSaved = bookletRepository.save(bookletResource)
        user.addBooklet(bookletSaved)
        return bookletSaved.toModel()
    }

    @Transactional
    override fun findBookletByIdWithTransactions(bookletId: UUID): Booklet? {
        val bookletResponse = bookletRepository.findByIdWithTransactions(bookletId)
        return bookletResponse?.toModel()
    }

    @Transactional
    override fun findBookletByLabelWithTransactions(userId: UserId, bookletLabel: String): Booklet? {
        val id = userId.value ?: return null
        return bookletRepository.findByOwnerAndLabelWithTransactions(id, bookletLabel)?.toModel()
    }

    @CacheEvict(cacheNames = ["allBooklets"], allEntries = true)
    @Transactional
    override fun deleteBookletById(bookletId: UUID) {
        val booklet = bookletRepository.findByIdWithRegularTransactions(bookletId) ?: return
        booklet.clearAllRegularTransactions()
        bookletRepository.deleteById(bookletId)
    }

    @Transactional
    override fun upsert(booklet: Booklet): Booklet {
        return bookletRepository.save(bookletMapper.asResource(booklet)).also {
            for(transaction in it.transactions) {
                transaction.booklet = it
            }
        }.toModel()
    }
    override fun update(booklet: Booklet) {
        val id = booklet.id ?: return
        bookletRepository.update(booklet.label, booklet.amount.value, id)
    }

    @Transactional
    override fun updateMonthlyPeriodStartDay(bookletId: UUID, monthlyPeriodStartDay: Int, monthlyPeriodEndDay: Int?): Boolean {
        return bookletRepository.updateMonthlyPeriodStartDay(bookletId, monthlyPeriodStartDay, monthlyPeriodEndDay) > 0
    }

    @Transactional
    override fun findBookletsForUser(userId: UserId): List<Booklet> {
        return userId.value?.let { id -> bookletRepository.findAllBookletsByUserId(id).map { it.toModel() } } ?: emptyList()
    }
}