package fr.sacane.jmanager.infrastructure.server.adapters

import fr.sacane.jmanager.domain.hexadoc.DatasourceAdapter
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.infrastructure.server.entity.AccountResource
import fr.sacane.jmanager.infrastructure.server.entity.CategoryResource
import fr.sacane.jmanager.infrastructure.server.repositories.AccountRepository
import fr.sacane.jmanager.infrastructure.server.repositories.CategoryRepository
import fr.sacane.jmanager.infrastructure.server.repositories.SheetRepository
import fr.sacane.jmanager.infrastructure.server.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
@DatasourceAdapter
class ServerTransactionAdapter(private val sheetRepository: SheetRepository) : TransactionRegister{

    companion object{
        private val LOGGER = LoggerFactory.getLogger("infra.server.adapters.ServerAdapter")
    }

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var accountRepository: AccountRepository
    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    override fun persist(userId: UserId, account: Account): User? {
        val user = userRepository.findById(userId.get()).orElse(null) ?: return null
        user.accounts?.add(account.asResource())
        return userRepository.save(user).toModel()
    }

    override fun findAccountByLabel(userId: UserId, labelAccount: String): Account? {
        val user = userRepository.findById(userId.get()).get()
        val account = user.accounts?.find { it.label == labelAccount } ?: return null
        return account.toModel()
    }

    @Transactional
    override fun persist(userId: UserId, accountLabel: String, sheet: Sheet): Sheet? {
        val user = userRepository.findById(userId.get()).get()
        val account = user.accounts?.find { it.label == accountLabel } ?: return null
        return try{
            account.sheets?.add(sheet.asResource())
            account.amount = sheet.accountAmount
            // =================================================
            userRepository.saveAndFlush(user)
            sheet
        }catch(e: Exception){
            null
        }
    }

    override fun persist(account: Account) :Account?{
        val accountGet = account.asResource()
        accountRepository.save(accountGet)
        accountRepository.flush()
        return accountRepository.findByLabel(account.label())!!.toModel()
    }

    override fun persist(userId: UserId, category: Category): Category? {
        val user = userRepository.findById(userId.get()).get()
        user.categories?.add(CategoryResource(category.label)) ?: return null
        userRepository.save(user)
        return category
    }

    override fun removeCategory(userId: UserId, labelCategory: String): Category? {
        val user = userRepository.findById(userId.get()).get()
        val category = user.categories?.find { it.label == labelCategory } ?: return null
        categoryRepository.deleteByLabel(labelCategory)
        return Category(category.label!!)
    }

    override fun remove(targetCategory: Category) {
        categoryRepository.deleteByLabel(targetCategory.label)
    }

    override fun findAccountById(accountId: Long): Account? {
        val accountResponse = accountRepository.findById(accountId)
        return accountResponse.get().toModel()
    }
    @Transactional
    override fun deleteAllSheets(accountID: Long, sheets: List<Long>) {
        try{

        }catch(e: Exception) {
            return
        }
    }

    override fun deleteAccountByID(accountID: Long) {
        accountRepository.deleteById(accountID)
    }
}
