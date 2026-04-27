package fr.sacane.jmanager.infrastructure.spi.adapters.transaction

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.asResource
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toModel
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.TagPersonalResource
import fr.sacane.jmanager.infrastructure.spi.entity.TransactionResource
import fr.sacane.jmanager.infrastructure.spi.repositories.BookletJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TagPersonalPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TransactionJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TransactionQueryJpaRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Month

@Service
@Adapter(Side.INFRASTRUCTURE)
class TransactionRepositoryJpaAdapter(
    private val transactionJpaRepository: TransactionJpaRepository,
    private val transactionQueryJpaRepository: TransactionQueryJpaRepository,
    private val bookletJpaRepository: BookletJpaRepository,
    private val tagRepository: DefaultTagPostgresRepository,
    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository
) : TransactionRepository {

    @Transactional
    override fun persist(userId: UserId, bookletLabel: String, transaction: Transaction): Transaction? {
        val id = userId.value ?: return null
        val booklet = bookletJpaRepository.findByOwnerAndLabelWithTransactions(id, bookletLabel) ?: return null
        val transactionResource: TransactionResource
        if (transaction.tag?.label == Tag.noneTag().label) {
            val noneTag = tagRepository.findUnknownTag()
            transactionResource = transaction.asResource(noneTag)
        } else if (transaction.tag?.isDefault == true) {
            val byName = tagRepository.findAll().firstOrNull { it.name == transaction.tag!!.label }
            transactionResource = if (byName != null) {
                transaction.asResource(byName)
            } else {
                transaction.mapToRightTag()
            }
        } else {
            transactionResource = transaction.mapToRightTag()
        }
        return try {
            transactionResource.booklet = booklet
            val saved = transactionJpaRepository.save(transactionResource)
            booklet.transactions.add(saved)
            booklet.amount = if (transactionResource.isIncome!!) transactionResource.value + booklet.amount else booklet.amount - transactionResource.value
            saved.toModel()
        } catch (_: Exception) {
            null
        }
    }

    fun Transaction.mapToRightTag(): TransactionResource {
        val tag = when {
            this.tag == null -> null
            this.tag!!.id != null -> {
                if (this.tag!!.isDefault) tagRepository.findByIdNullable(this.tag!!.id!!) else tagPersonalPostgresRepository.findByIdNullable(this.tag!!.id!!)
            }
            this.tag!!.isDefault -> {
                tagRepository.findAll().firstOrNull { it.name == this.tag!!.label } ?: (this.tag!!.asResource() as DefaultTagResource)
            }
            else -> null
        }
        return this.asResource(tag)
    }
    @Transactional
    override fun deleteAllTransactionsById(transactionIds: List<java.util.UUID>) {
        if (transactionIds.isEmpty()) return
        transactionJpaRepository.deleteAllByIdTransactionIn(transactionIds)
    }

    override fun findTransactionById(transactionId: java.util.UUID): Transaction? {
        return transactionJpaRepository.findTransactionResourceByIdTransaction(transactionId)?.toModel()
    }


    @Transactional
    override fun save(bookletId: java.util.UUID, transaction: Transaction): Transaction? {
        val tag = if (transaction.tag == null) {
            tagRepository.findUnknownTag()
        } else if (transaction.tag!!.isDefault) {
            tagRepository.findAll().firstOrNull { it.name == transaction.tag!!.label }
        } else {
            tagPersonalPostgresRepository.findByIdNullable(transaction.tag?.id!!)
        }

        val existingResource = transaction.id?.let {
            transactionJpaRepository.findTransactionResourceByIdTransaction(it)
        }

        val transactionResource = if (existingResource != null) {
            existingResource.label = transaction.label
            existingResource.date = transaction.date
            existingResource.value = transaction.amount.value
            existingResource.isIncome = transaction.isIncome
            existingResource.lastModified = transaction.lastModified
            existingResource.isPreview = transaction.isPreview
            existingResource.regularTransactionId = transaction.regularTransactionId?.value?.let {
                try { java.util.UUID.fromString(it) } catch (_: IllegalArgumentException) { null }
            }
            existingResource.tag = null
            existingResource.personalTag = null
            if (tag != null) {
                when (tag) {
                    is DefaultTagResource -> existingResource.tag = tag
                    is TagPersonalResource -> existingResource.personalTag = tag
                }
            }
            existingResource
        } else {
            transaction.asResource(tag)
        }

        transactionResource.booklet = bookletJpaRepository.findByIdOrNull(bookletId)
        return transactionJpaRepository.save(transactionResource).toModel()
    }

    @Transactional
    override fun findBookletByLabelWithTransactions(label: String, userId: UserId): Booklet? {
        if(userId.value == null) return null
        return bookletJpaRepository.findTransactionsByLabelForUser(label, userId.value!!)
            ?.toModel()
    }

    override fun findBookletByIdWithTransactions(id: java.util.UUID): Booklet? {
        return bookletJpaRepository.findTransactionsById(id)?.toModel()
    }

    override fun findTransactionsByBookletId(bookletId: java.util.UUID): List<Transaction>? {
        return bookletJpaRepository.findTransactionsById(bookletId)?.transactions?.map { it.toModel() }
    }

    override fun findTransactionsByBookletYearAndMonth(
        bookletId: java.util.UUID,
        year: Int,
        month: Month
    ): List<Transaction> {
        val from = java.time.LocalDate.of(year, month, 1)
        val to = from.withDayOfMonth(from.lengthOfMonth())
        return transactionQueryJpaRepository.findByBookletIdAndDateBetween(bookletId, from, to)
            .map { it.toModel() }
    }

    @Transactional
    override fun isPersonalTagUsed(tagId: java.util.UUID): Boolean {
        return transactionJpaRepository.existsByPersonalTagId(tagId)
    }

    @Transactional
    override fun replacePersonalTagByDefault(tagId: java.util.UUID, defaultTag: Tag) {
        val defaultTagResource = tagRepository.findAll().firstOrNull { it.name == defaultTag.label } ?: return
        transactionJpaRepository.replacePersonalTagByDefaultId(tagId, defaultTagResource.idTag!!)
    }
}