package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.TransactionRepositoryPort
import fr.sacane.jmanager.infrastructure.spi.entity.SheetResource
import fr.sacane.jmanager.infrastructure.spi.repositories.*
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service


@Service
@Adapter(Side.INFRASTRUCTURE)
class SqlTransactionAdapter(
    private val sheetRepository: SheetRepository,
    private val accountJpaRepository: AccountJpaRepository,
    private val tagRepository: DefaultTagPostgresRepository,
    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository
) : TransactionRepositoryPort{

    @Transactional
    override fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction? {
        val id = userId.id ?: return null
        val account = accountJpaRepository.findByOwnerAndLabelWithSheets(id, accountLabel) ?: return null
        val sheetResource: SheetResource
        if(transaction.tag.label == "Aucune"){
            val noneTag = tagRepository.findUnknownTag()
            sheetResource = transaction.asResource(noneTag)
        } else {
            sheetResource = transaction.mapToRightTag()
        }
        return try{
            val saved = sheetRepository.save(sheetResource)
            account.sheets.add(saved)
            account.amount = if(sheetResource.isIncome!!) sheetResource.value + account.amount else account.amount - sheetResource.value
            transaction
        }catch(e: Exception){
            null
        }
    }

    @Transactional
    override fun saveAllSheets(transactions: List<Transaction>) {
        sheetRepository.saveAll(transactions.map { it.mapToRightTag() })
    }
    private fun Transaction.mapToRightTag(): SheetResource {
        val tag = this.tag.id?.let {
            if(this.tag.isDefault) {
                tagRepository.findByIdNullable(it)
            } else {
                tagPersonalPostgresRepository.findByIdNullable(it)
            }
        }
        return this.asResource(tag)
    }
    @Transactional
    override fun deleteAllSheetsById(sheetIds: List<Long>) {
        sheetRepository.deleteAllById(sheetIds)
    }
    @Transactional
    override fun findTransactionById(transactionId: Long): Transaction? {
        return sheetRepository.findSheetResourceByIdSheet(transactionId)?.toModel()
    }
    @Transactional
    override fun save(transaction: Transaction): Transaction? {
        return sheetRepository.save(transaction.asResource(transaction.tag.asResource())).toModel()
    }

    @Transactional
    override fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Account? {
        if(userId.id == null) return null
        return accountJpaRepository.findSheetsByLabelAndAccountOf(label, userId.id!!)
            ?.toModel()
    }
}
