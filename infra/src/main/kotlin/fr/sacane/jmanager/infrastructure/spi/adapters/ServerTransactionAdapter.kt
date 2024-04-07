package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.infrastructure.spi.entity.TagResource
import fr.sacane.jmanager.infrastructure.spi.repositories.*
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service


@Service
@Adapter(Side.DATASOURCE)
class ServerTransactionAdapter(
    private val sheetRepository: SheetRepository,
    private val userPostgresRepository: UserPostgresRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val accountMapper: AccountMapper
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
        println("try to persist it")
        val id = userId.id ?: return null
        val account = accountRepository.findByOwnerAndLabelWithSheets(id, accountLabel) ?: return null
        return try{
            println("conversion")
            val sheetResource = transaction.asResource()
            val saved = sheetRepository.save(sheetResource)
            account.sheets.add(saved)
            account.amount = transaction.sold.applyOnValue { it }
            transaction
        }catch(e: Exception){
            println("error ?")
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
    override fun persist(userId: UserId, category: Tag): Tag? {
        val id = userId.id ?: return null
        val user = userPostgresRepository.findById(id).orElseThrow()
        user.tags.add(TagResource(name = category.label))
        userPostgresRepository.save(user)
        return category
    }
    @Transactional
    override fun removeCategory(userId: UserId, labelCategory: String): Tag? {
        val id = userId.id ?: return null
        val user = userPostgresRepository.findById(id).orElseThrow()
        val category = user.categories.find { it.label == labelCategory } ?: return null
        categoryRepository.deleteByLabel(labelCategory)
        return Tag(category.label)
    }
    @Transactional
    override fun remove(targetCategory: Tag) {
        categoryRepository.deleteByLabel(targetCategory.label)
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
        sheetRepository.saveAll(transactions.map { it.asResource() })
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
