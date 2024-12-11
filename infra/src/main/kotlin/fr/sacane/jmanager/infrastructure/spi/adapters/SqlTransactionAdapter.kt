package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.TransactionRepositoryPort
import fr.sacane.jmanager.infrastructure.spi.entity.TransactionResource
import fr.sacane.jmanager.infrastructure.spi.repositories.*
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service


@Service
@Adapter(Side.INFRASTRUCTURE)
class SqlTransactionAdapter(
    private val transactionJpaRepository: TransactionJpaRepository,
    private val accountJpaRepository: AccountJpaRepository,
    private val tagRepository: DefaultTagPostgresRepository,
    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository
) : TransactionRepositoryPort{

    @Transactional
    override fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction? {
        val id = userId.value ?: return null
        val account = accountJpaRepository.findByOwnerAndLabelWithSheets(id, accountLabel) ?: return null
        val transactionResource: TransactionResource
        if(transaction.tag.label == "Aucune"){
            val noneTag = tagRepository.findUnknownTag()
            transactionResource = transaction.asResource(noneTag)
        } else {
            transactionResource = transaction.mapToRightTag()
        }
        return try{
            val saved = transactionJpaRepository.save(transactionResource)
            account.sheets.add(saved)
            account.amount = if(transactionResource.isIncome!!) transactionResource.value + account.amount else account.amount - transactionResource.value
            transaction
        }catch(e: Exception){
            null
        }
    }

    @Transactional
    override fun saveAllSheets(transactions: List<Transaction>) {
        transactionJpaRepository.saveAll(transactions.map { it.mapToRightTag() })
    }
    fun Transaction.mapToRightTag(): TransactionResource {
        val tag = this.tag.id?.let {
            if(this.tag.isDefault) {
                tagRepository.findByIdNullable(it)
            } else {
                tagPersonalPostgresRepository.findByIdNullable(it)
            }
        }
        return this.asResource(tag)
    }

    override fun deleteAllSheetsById(sheetIds: List<Long>) {
        transactionJpaRepository.deleteAllById(sheetIds)
    }

    override fun findTransactionById(transactionId: Long): Transaction? {
        return transactionJpaRepository.findSheetResourceByIdSheet(transactionId)?.toModel()
    }


    override fun save(accountId: Long, transaction: Transaction): Transaction? {
        val tag = if(transaction.tag.isDefault){
            tagRepository.findByName(transaction.tag.label)
        } else {
            tagPersonalPostgresRepository.findByIdNullable(transaction.tag.id!!)
        }
        val transactionResource = transaction.asResource(tag)
        transactionResource.account = accountJpaRepository.findByIdOrNull(accountId)
        return transactionJpaRepository.save(transactionResource).toModel()
    }

    @Transactional
    override fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Account? {
        if(userId.value == null) return null
        return accountJpaRepository.findSheetsByLabelAndAccountOf(label, userId.value!!)
            ?.toModel()
    }
}
