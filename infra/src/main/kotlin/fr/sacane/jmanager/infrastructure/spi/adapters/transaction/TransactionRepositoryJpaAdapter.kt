package fr.sacane.jmanager.infrastructure.spi.adapters.transaction

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.asResource
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toModel
import fr.sacane.jmanager.infrastructure.spi.entity.DefaultTagResource
import fr.sacane.jmanager.infrastructure.spi.entity.TransactionResource
import fr.sacane.jmanager.infrastructure.spi.repositories.BookletJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.DefaultTagPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TagPersonalPostgresRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.TransactionJpaRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Month

@Service
@Adapter(Side.INFRASTRUCTURE)
class TransactionRepositoryJpaAdapter(
    private val transactionJpaRepository: TransactionJpaRepository,
    private val bookletJpaRepository: BookletJpaRepository,
    private val tagRepository: DefaultTagPostgresRepository,
    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository
) : TransactionRepository {

    @Transactional
    override fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction? {
        val id = userId.value ?: return null
        val account = bookletJpaRepository.findByOwnerAndLabelWithSheets(id, accountLabel) ?: return null
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
            transactionResource.account = account
            val saved = transactionJpaRepository.save(transactionResource)
            account.sheets.add(saved)
            account.amount = if (transactionResource.isIncome!!) transactionResource.value + account.amount else account.amount - transactionResource.value
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

    override fun deleteAllSheetsById(sheetIds: List<java.util.UUID>) {
        transactionJpaRepository.deleteAllById(sheetIds)
    }

    override fun findTransactionById(transactionId: java.util.UUID): Transaction? {
        return transactionJpaRepository.findSheetResourceByIdSheet(transactionId)?.toModel()
    }


    @Transactional
    override fun save(accountId: java.util.UUID, transaction: Transaction): Transaction? {
        val tag = if (transaction.tag == null) {
            tagRepository.findUnknownTag()
        } else if (transaction.tag!!.isDefault) {
            tagRepository.findAll().firstOrNull { it.name == transaction.tag!!.label }
        } else {
            tagPersonalPostgresRepository.findByIdNullable(transaction.tag?.id!!)
        }
        val transactionResource = transaction.asResource(tag)
        transactionResource.account = bookletJpaRepository.findByIdOrNull(accountId)
        return transactionJpaRepository.save(transactionResource).toModel()
    }

    @Transactional
    override fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Booklet? {
        if(userId.value == null) return null
        return bookletJpaRepository.findSheetsByLabelAndAccountOf(label, userId.value!!)
            ?.toModel()
    }

    override fun findAccountWithTransactionById(id: java.util.UUID): Booklet? {
        return bookletJpaRepository.findTransactionsById(id)?.toModel()
    }

    override fun findTransactionsByBookletId(bookletId: java.util.UUID): List<Transaction>? {
        return bookletJpaRepository.findTransactionsById(bookletId)?.sheets?.map { it.toModel() }
    }

    override fun findTransactionsByBookletYearAndMonth(
        bookletId: java.util.UUID,
        year: Int,
        month: Month
    ): List<Transaction>? {
        return bookletJpaRepository.findTransactionsById(bookletId)?.sheets?.filter {
            it.date.year == year && it.date.month == month
        }?.map { it.toModel()}
    }
}