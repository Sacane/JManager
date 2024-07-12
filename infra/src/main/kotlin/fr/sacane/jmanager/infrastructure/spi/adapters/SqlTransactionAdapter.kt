package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.infrastructure.spi.entity.SheetResource
import fr.sacane.jmanager.infrastructure.spi.repositories.*
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service


@Service
@Adapter(Side.INFRASTRUCTURE)
class SqlTransactionAdapter(
    private val sheetRepository: SheetRepository,
    private val userPostgresRepository: UserPostgresRepository,
    private val accountRepository: AccountRepository,
    private val accountMapper: AccountMapper,
    private val tagRepository: DefaultTagPostgresRepository,
    private val tagPersonalPostgresRepository: TagPersonalPostgresRepository
) : TransactionRegister{

    @Transactional
    override fun persist(userId: UserId, account: Account): User? {
        val id = userId.id ?: return null
        val user = userPostgresRepository.findByIdWithAccount(id) ?: return null
        user.addAccount(accountMapper.asResource(account))
        return userPostgresRepository.save(user).toModelWithSimpleAccounts()
    }
    @Transactional
    override fun findAccountByLabel(userId: UserId, labelAccount: String): Account? {
        val id = userId.id ?: return null
        return accountRepository.findByOwnerAndLabelWithSheets(id, labelAccount)
            ?.toModel()
    }

    @Transactional
    override fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction? {
        val id = userId.id ?: return null
        val account = accountRepository.findByOwnerAndLabelWithSheets(id, accountLabel) ?: return null
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
            account.amount = transaction.sold.applyOnValue { it }
            transaction
        }catch(e: Exception){
            null
        }
    }
    @Transactional
    override fun persist(account: Account) :Account?{
        val accountGet = accountMapper.asResource(account)
        val registered = accountRepository.save(accountGet)
        return registered.toModel()
    }
    @Transactional
    override fun persist(userId: UserId, tag: Tag): Tag? {
        val id = userId.id ?: return null
        val user = userPostgresRepository.findById(id).orElseThrow()
        user.tags.add(tag.toPersonalTag())
        userPostgresRepository.save(user)
        return tag
    }

    @Transactional
    override fun remove(targetCategory: Tag) {
        tagRepository.deleteByName(targetCategory.label)
    }
    @Transactional
    override fun findAccountById(accountId: Long): Account? {
        val accountResponse = accountRepository.findByIdWithSheets(accountId)
        return accountResponse?.toModel()
    }
    @Transactional
    override fun deleteAccountByID(accountID: Long) {
        accountRepository.deleteById(accountID)
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
    override fun findSheetByID(sheetID: Long): Transaction? {
        return sheetRepository.findSheetResourceByIdSheet(sheetID)?.toModel()
    }
    @Transactional
    override fun save(transaction: Transaction): Transaction? {
        return sheetRepository.save(transaction.asResource()).toModel()
    }
    @Transactional
    override fun save(account: Account): Account? {
        return accountRepository.save(account.asResource()).toModel()
    }
    @Transactional
    override fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Account? {
        if(userId.id == null) return null
        return accountRepository.findSheetsByLabelAndAccountOf(label, userId.id!!)
            ?.toModel()
    }
}
