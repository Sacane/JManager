package fr.sacane.jmanager.infrastructure.postgres.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.infrastructure.postgres.entity.CategoryResource
import fr.sacane.jmanager.infrastructure.postgres.repositories.AccountRepository
import fr.sacane.jmanager.infrastructure.postgres.repositories.CategoryRepository
import fr.sacane.jmanager.infrastructure.postgres.repositories.SheetRepository
import fr.sacane.jmanager.infrastructure.postgres.repositories.UserPostgresRepository
import org.springframework.stereotype.Service


@Service
@Adapter(Side.DATASOURCE)
class ServerTransactionAdapter(
    private val sheetRepository: SheetRepository,
    private val userPostgresRepository: UserPostgresRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
    ) : TransactionRegister{

    override fun persist(userId: UserId, account: Account): User? {
        val id = userId.id ?: return null
        val user = userPostgresRepository.findById(id).orElseThrow()
        user.accounts.add(account.asResource())
        return userPostgresRepository.save(user).toModel()
    }

    override fun findAccountByLabel(userId: UserId, labelAccount: String): Account? {
        val id = userId.id ?: return null
        val user = userPostgresRepository.findById(id).orElseThrow()
        val account = user.accounts.find { it.label == labelAccount } ?: return null
        return account.toModel()
    }

    override fun persist(userId: UserId, accountLabel: String, sheet: Sheet): Sheet? {
        val id = userId.id ?: return null
        val user = userPostgresRepository.findById(id).orElseThrow()
        val account = user.accounts.find { it.label == accountLabel } ?: return null
        return try{
            account.sheets.add(sheet.asResource())
            account.amount = sheet.sold
            // =================================================
            userPostgresRepository.saveAndFlush(user)
            sheet
        }catch(e: Exception){
            null
        }
    }

    override fun persist(account: Account) :Account?{
        val accountGet = account.asResource()
        val registered = accountRepository.save(accountGet)
        accountRepository.flush()
        return registered.toModel()
    }

    override fun persist(userId: UserId, category: Category): Category? {
        val id = userId.id ?: return null
        val user = userPostgresRepository.findById(id).orElseThrow()
        user.categories.add(CategoryResource(label = category.label))
        userPostgresRepository.save(user)
        return category
    }

    override fun removeCategory(userId: UserId, labelCategory: String): Category? {
        val id = userId.id ?: return null
        val user = userPostgresRepository.findById(id).orElseThrow()
        val category = user.categories.find { it.label == labelCategory } ?: return null
        categoryRepository.deleteByLabel(labelCategory)
        return Category(category.label)
    }

    override fun remove(targetCategory: Category) {
        categoryRepository.deleteByLabel(targetCategory.label)
    }

    override fun findAccountById(accountId: Long): Account? {
        val accountResponse = accountRepository.findById(accountId)
        return accountResponse.get().toModel()
    }

    override fun deleteAccountByID(accountID: Long) {
        accountRepository.deleteById(accountID)
    }

    override fun saveAllSheets(sheets: List<Sheet>) {
        sheetRepository.saveAll(sheets.map { it.asResource() })
    }

    override fun deleteAllSheetsById(sheetIds: List<Long>) {
        sheetRepository.deleteAllById(sheetIds)
    }

    override fun findSheetByID(sheetID: Long): Sheet? {
        return sheetRepository.findSheetResourceByIdSheet(sheetID)?.toModel()
    }

    override fun save(sheet: Sheet): Sheet? {
        return sheetRepository.save(sheet.asResource()).toModel()
    }

    override fun save(account: Account): Account? {
        return accountRepository.save(account.asResource()).toModel()
    }
}
